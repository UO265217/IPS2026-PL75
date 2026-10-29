package giis.ips.reservas.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.reservas.FranjaDTO;
import giis.ips.reservas.ReservaEntity;
import giis.ips.reservas.ReservaModel;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 35206 (reservas de instalaciones por usuarios externos) sobre el modelo y la base de datos.
 * Parten de los datos de ejemplo de data.sql y fijan como dia actual el jueves 29/10/2026:
 * - Campo de entrenamiento 1: lunes 2/11 entrenamiento 10:00-12:00 y reserva 18:00-19:30; martes 3/11 libre;
 *   miercoles 4/11 entrenamiento 7:00-9:00; jueves 5/11 entrenamientos 8:00-9:00 y 11:15-12:00.
 * - Estadio: domingo 8/11 partido 17:00-19:00.
 * Las franjas se resumen como "inicio-fin" en minutos para compararlas de una vez.
 */
class TestReserva {
	private static final LocalDate HOY = LocalDate.parse("2026-10-29");
	private static final String CAMPO = "Campo de entrenamiento 1";
	private static final String ESTADIO = "Estadio";
	private static final String LUNES_2_NOV = "2026-11-02";
	private static final String MARTES_3_NOV = "2026-11-03";
	private static final String MIERCOLES_4_NOV = "2026-11-04";
	private static final String JUEVES_5_NOV = "2026-11-05";
	private static final String DOMINGO_8_NOV = "2026-11-08";

	private Database db = new Database();
	private ReservaModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new ReservaModel(HOY);
	}

	private int idDe(String instalacion) {
		return (Integer) db.executeQueryArray("select id from instalacion where nombre = ?", instalacion).get(0)[0];
	}

	private ReservaEntity crear(String instalacion, String fecha, int horaInicio, int horaFin) {
		ReservaEntity r = new ReservaEntity();
		r.setInstalacionId(idDe(instalacion));
		r.setFecha(fecha);
		r.setHoraInicio(horaInicio);
		r.setHoraFin(horaFin);
		r.setNombreUsuario("Ana Torres");
		r.setNumeroTarjeta("4111111111111111");
		return r;
	}

	private void reservar(String instalacion, String fecha, int horaInicio, int horaFin) {
		model.reservar(crear(instalacion, fecha, horaInicio, horaFin));
	}

	private int contarReservas() {
		return db.executeQueryArray("select id from reserva").size();
	}

	/** Comprueba el mensaje y que no se ha guardado ninguna reserva */
	private void assertError(String mensaje, Runnable accion) {
		int antes = contarReservas();
		ApplicationException ex = assertThrows(ApplicationException.class, accion::run);
		assertEquals(mensaje, ex.getMessage());
		assertEquals(antes, contarReservas(), "Si hay error no se debe guardar nada");
	}

	private String libres(String instalacion, String fecha) {
		StringBuilder sb = new StringBuilder();
		for (FranjaDTO f : model.getFranjasLibres(idDe(instalacion), LocalDate.parse(fecha)))
			sb.append(sb.length() == 0 ? "" : " ").append(f.getHoraInicio()).append("-").append(f.getHoraFin());
		return sb.toString();
	}

	// ---- Horas disponibles ----

	@Test
	void diaSinUsosTodoLibre() {
		assertEquals("480-1320", libres(CAMPO, MARTES_3_NOV));
	}

	@Test
	void entrenamientoConMargenYReserva() {
		// Entrenamiento 10:00-12:00 (bloquea hasta 13:30) y reserva 18:00-19:30
		assertEquals("480-600 810-1080 1170-1320", libres(CAMPO, LUNES_2_NOV));
	}

	@Test
	void partidoConMargen() {
		// Partido 17:00-19:00: no se puede reservar hasta las 20:30
		assertEquals("480-1020 1230-1320", libres(ESTADIO, DOMINGO_8_NOV));
	}

	@Test
	void usoAntesDeAbrirRetrasaLaApertura() {
		// Entrenamiento 7:00-9:00: el margen llega hasta las 10:30
		assertEquals("630-1320", libres(CAMPO, MIERCOLES_4_NOV));
	}

	@Test
	void huecoDeMenosDeUnaHoraNoEsFranjaLibre() {
		// Entre las 10:30 (margen del entrenamiento de 8:00-9:00) y las 11:15 solo hay 45 minutos
		assertEquals("810-1320", libres(CAMPO, JUEVES_5_NOV));
	}

	@Test
	void otraInstalacionNoAfecta() {
		assertEquals("480-1320", libres(ESTADIO, LUNES_2_NOV));
	}

	@Test
	void laNuevaReservaDejaDeEstarLibre() {
		reservar(CAMPO, MARTES_3_NOV, 600, 720);
		assertEquals("480-600 720-1320", libres(CAMPO, MARTES_3_NOV));
	}

	@Test
	void franjasLibresDeInstalacionInexistente() {
		ApplicationException ex = assertThrows(ApplicationException.class,
				() -> model.getFranjasLibres(-1, LocalDate.parse(MARTES_3_NOV)));
		assertEquals("No existe la instalación seleccionada", ex.getMessage());
	}

	// ---- Reservas validas ----

	@Test
	void reservarGuardaTodosLosDatos() {
		ReservaEntity r = crear(CAMPO, MARTES_3_NOV, 617, 677);
		r.setNombreUsuario("  Ana Torres  ");
		r.setImporte(1);
		model.reservar(r);
		List<ReservaEntity> reservas = model.getReservas(idDe(CAMPO), LocalDate.parse(MARTES_3_NOV));
		assertEquals(1, reservas.size());
		ReservaEntity guardada = reservas.get(0);
		assertEquals(idDe(CAMPO), guardada.getInstalacionId());
		assertEquals(MARTES_3_NOV, guardada.getFecha());
		assertEquals(617, guardada.getHoraInicio());
		assertEquals(677, guardada.getHoraFin());
		assertEquals("Ana Torres", guardada.getNombreUsuario());
		assertEquals("4111111111111111", guardada.getNumeroTarjeta());
		assertEquals(50.0, guardada.getImporte(), "El importe lo calcula el modelo e ignora el recibido");
		assertEquals(50.0, r.getImporte());
	}

	@Test
	void reservaDeApertura() {
		reservar(CAMPO, MARTES_3_NOV, 480, 540);
		assertEquals(1, model.getReservas(idDe(CAMPO), LocalDate.parse(MARTES_3_NOV)).size());
	}

	@Test
	void reservaHastaElCierre() {
		reservar(CAMPO, MARTES_3_NOV, 1260, 1320);
		assertEquals(1, model.getReservas(idDe(CAMPO), LocalDate.parse(MARTES_3_NOV)).size());
	}

	@Test
	void reservaDeTodoElDia() {
		reservar(CAMPO, MARTES_3_NOV, 480, 1320);
		assertEquals(700.0, model.getReservas(idDe(CAMPO), LocalDate.parse(MARTES_3_NOV)).get(0).getImporte());
	}

	@Test
	void reservaQueAcabaCuandoEmpiezaElEntrenamiento() {
		reservar(CAMPO, LUNES_2_NOV, 540, 600);
		assertEquals(2, model.getReservas(idDe(CAMPO), LocalDate.parse(LUNES_2_NOV)).size());
	}

	@Test
	void reservaJustoAlAcabarElMargen() {
		reservar(CAMPO, LUNES_2_NOV, 810, 870);
		assertEquals(2, model.getReservas(idDe(CAMPO), LocalDate.parse(LUNES_2_NOV)).size());
	}

	@Test
	void reservasPegadasAOtraReserva() {
		reservar(CAMPO, LUNES_2_NOV, 1020, 1080);
		reservar(CAMPO, LUNES_2_NOV, 1170, 1230);
		assertEquals(3, model.getReservas(idDe(CAMPO), LocalDate.parse(LUNES_2_NOV)).size());
	}

	@Test
	void reservaParaHoy() {
		reservar(CAMPO, HOY.toString(), 600, 660);
		assertEquals(1, model.getReservas(idDe(CAMPO), HOY).size());
	}

	// ---- Importe: 50 euros por hora empezada ----

	@ParameterizedTest
	@CsvSource({ "480, 540, 50", "480, 541, 100", "480, 570, 100", "480, 600, 100", "480, 601, 150",
			"480, 1320, 700" })
	void importePorHoraEmpezada(int horaInicio, int horaFin, double importe) {
		assertEquals(importe, ReservaModel.calcularImporte(horaInicio, horaFin));
	}

	// ---- Datos de la reserva ----

	@Test
	void instalacionInexistente() {
		ReservaEntity r = crear(CAMPO, MARTES_3_NOV, 600, 660);
		r.setInstalacionId(-1);
		assertError("No existe la instalación seleccionada", () -> model.reservar(r));
	}

	@ParameterizedTest
	@CsvSource(value = { "NULL", "''", "'   '" }, nullValues = "NULL")
	void sinNombreDeUsuario(String nombre) {
		ReservaEntity r = crear(CAMPO, MARTES_3_NOV, 600, 660);
		r.setNombreUsuario(nombre);
		assertError("Hay que indicar el nombre del usuario que solicita la reserva", () -> model.reservar(r));
	}

	@ParameterizedTest
	@CsvSource(value = { "NULL", "''", "'   '" }, nullValues = "NULL")
	void sinNumeroDeTarjeta(String tarjeta) {
		ReservaEntity r = crear(CAMPO, MARTES_3_NOV, 600, 660);
		r.setNumeroTarjeta(tarjeta);
		assertError("Hay que indicar el número de tarjeta bancaria para el cobro", () -> model.reservar(r));
	}

	@Test
	void fechaPasada() {
		assertError("No se puede reservar en una fecha pasada",
				() -> reservar(CAMPO, HOY.minusDays(1).toString(), 600, 660));
	}

	// ---- Duracion y horario ----

	@ParameterizedTest
	@CsvSource({ "600, 659", "600, 600", "660, 600" })
	void menosDeUnaHora(int horaInicio, int horaFin) {
		assertError("La reserva debe durar al menos 1 hora", () -> reservar(CAMPO, MARTES_3_NOV, horaInicio, horaFin));
	}

	@ParameterizedTest
	@CsvSource({ "479, 540", "1260, 1321", "420, 1380" })
	void fueraDelHorarioDeOchoAVeintidos(int horaInicio, int horaFin) {
		assertError("Solo se puede reservar entre las 8:00 y las 22:00",
				() -> reservar(CAMPO, MARTES_3_NOV, horaInicio, horaFin));
	}

	// ---- Usos de los equipos y otras reservas ----

	@ParameterizedTest
	@CsvSource({ "540, 601", "660, 780", "540, 780", "630, 690" })
	void coincideConUnEntrenamiento(int horaInicio, int horaFin) {
		// Entrenamiento del lunes de 10:00 a 12:00: solape por delante, por detras, conteniendolo y dentro
		assertError("La instalación está ocupada por un equipo en esa franja",
				() -> reservar(CAMPO, LUNES_2_NOV, horaInicio, horaFin));
	}

	@Test
	void coincideConUnPartido() {
		assertError("La instalación está ocupada por un equipo en esa franja",
				() -> reservar(ESTADIO, DOMINGO_8_NOV, 960, 1080));
	}

	@ParameterizedTest
	@CsvSource({ "720, 780", "809, 869" })
	void dentroDelMargenTrasUnEntrenamiento(int horaInicio, int horaFin) {
		// El entrenamiento acaba a las 12:00: no se puede empezar antes de las 13:30
		assertError("No se puede reservar hasta pasada 1 hora y media desde que termina el uso de un equipo",
				() -> reservar(CAMPO, LUNES_2_NOV, horaInicio, horaFin));
	}

	@Test
	void dentroDelMargenTrasUnPartido() {
		// El partido acaba a las 19:00: no se puede empezar antes de las 20:30
		assertError("No se puede reservar hasta pasada 1 hora y media desde que termina el uso de un equipo",
				() -> reservar(ESTADIO, DOMINGO_8_NOV, 1229, 1289));
	}

	@Test
	void margenDeUsoAntesDeAbrir() {
		// Entrenamiento del miercoles de 7:00 a 9:00: el margen llega hasta las 10:30
		assertError("No se puede reservar hasta pasada 1 hora y media desde que termina el uso de un equipo",
				() -> reservar(CAMPO, MIERCOLES_4_NOV, 570, 630));
	}

	@ParameterizedTest
	@CsvSource({ "1050, 1110", "1140, 1200", "1020, 1260", "1100, 1160" })
	void coincideConOtraReserva(int horaInicio, int horaFin) {
		// Reserva del lunes de 18:00 a 19:30: solape por delante, por detras, conteniendola y dentro
		assertError("La instalación ya tiene otra reserva en esa franja",
				() -> reservar(CAMPO, LUNES_2_NOV, horaInicio, horaFin));
	}
}
