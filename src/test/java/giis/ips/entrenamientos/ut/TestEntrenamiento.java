package giis.ips.entrenamientos.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.entrenamientos.EntrenamientoEntity;
import giis.ips.entrenamientos.EntrenamientoModel;
import giis.ips.entrenamientos.UsoInstalacionDTO;
import giis.ips.reservas.FranjaDTO;
import giis.ips.reservas.ReservaModel;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 36124 (asignar entrenamientos a instalaciones) sobre el modelo y la base de datos.
 * Parten de los datos de ejemplo de data.sql y fijan como dia actual el jueves 29/10/2026:
 * - Campo de entrenamiento 1, lunes 2/11: jardineria 8:00-9:00, entrenamiento del Juvenil A 10:00-12:00 y
 *   reserva externa 18:00-19:30. Martes 3/11 libre.
 * - Estadio: martes 3/11 jardineria 9:00-11:00; domingo 8/11 partido del Primer equipo 17:00-19:00.
 * Los usos de un dia se resumen como "TIPO inicio-fin" para compararlos de una vez.
 */
class TestEntrenamiento {
	private static final LocalDate HOY = LocalDate.parse("2026-10-29");
	private static final String CAMPO = "Campo de entrenamiento 1";
	private static final String ESTADIO = "Estadio";
	private static final String PRIMER_EQUIPO = "Primer equipo";
	private static final String LUNES_2_NOV = "2026-11-02";
	private static final String MARTES_3_NOV = "2026-11-03";
	private static final String DOMINGO_8_NOV = "2026-11-08";

	/** Lo que hay en el campo el lunes 2/11 en los datos de ejemplo */
	private static final String CAMPO_LUNES = "JARDINERIA 480-540 ENTRENAMIENTO 600-720 RESERVA 1080-1170";

	private Database db = new Database();
	private EntrenamientoModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new EntrenamientoModel(HOY);
	}

	private int idDe(String tabla, String nombre) {
		return (Integer) db.executeQueryArray("select id from " + tabla + " where nombre = ?", nombre).get(0)[0];
	}

	private EntrenamientoEntity crear(String instalacion, String fecha, int horaInicio, int horaFin) {
		EntrenamientoEntity e = new EntrenamientoEntity();
		e.setEquipoId(idDe("equipo", PRIMER_EQUIPO));
		e.setInstalacionId(idDe("instalacion", instalacion));
		e.setFecha(fecha);
		e.setHoraInicio(horaInicio);
		e.setHoraFin(horaFin);
		return e;
	}

	/** Asigna el entrenamiento y devuelve lo eliminado en formato "TIPO inicio-fin" */
	private String asignar(String instalacion, String fecha, int horaInicio, int horaFin) {
		return resumir(model.asignar(crear(instalacion, fecha, horaInicio, horaFin)));
	}

	private String usos(String instalacion, String fecha) {
		return resumir(model.getUsos(idDe("instalacion", instalacion), LocalDate.parse(fecha)));
	}

	private static String resumir(List<UsoInstalacionDTO> usos) {
		StringBuilder sb = new StringBuilder();
		for (UsoInstalacionDTO u : usos)
			sb.append(sb.length() == 0 ? "" : " ").append(u.getTipo()).append(" ").append(u.getHoraInicio()).append("-")
					.append(u.getHoraFin());
		return sb.toString();
	}

	/** Comprueba el mensaje y que no se ha guardado ni eliminado nada */
	private void assertError(String mensaje, EntrenamientoEntity e) {
		String antes = usos(CAMPO, LUNES_2_NOV) + "|" + usos(ESTADIO, DOMINGO_8_NOV) + "|" + usos(CAMPO, MARTES_3_NOV);
		ApplicationException ex = assertThrows(ApplicationException.class, () -> model.asignar(e));
		assertEquals(mensaje, ex.getMessage());
		assertEquals(antes, usos(CAMPO, LUNES_2_NOV) + "|" + usos(ESTADIO, DOMINGO_8_NOV) + "|" + usos(CAMPO, MARTES_3_NOV),
				"Si hay error no se debe guardar ni eliminar nada");
	}

	// ---- Asignaciones sin nada que eliminar ----

	@Test
	void asignarGuardaTodosLosDatos() {
		assertEquals("", asignar(CAMPO, MARTES_3_NOV, 617, 735));
		List<Object[]> filas = db.executeQueryArray("select equipoId, instalacionId, fecha, horaInicio, horaFin"
				+ " from entrenamiento where fecha = ?", MARTES_3_NOV);
		assertEquals(1, filas.size());
		assertEquals(idDe("equipo", PRIMER_EQUIPO), filas.get(0)[0]);
		assertEquals(idDe("instalacion", CAMPO), filas.get(0)[1]);
		assertEquals(MARTES_3_NOV, filas.get(0)[2]);
		assertEquals(617, filas.get(0)[3]);
		assertEquals(735, filas.get(0)[4]);
	}

	@Test
	void entrenamientoPegadoAOtroEntrenamiento() {
		// Pegados no coinciden: 9:00-10:00 y 12:00-13:00 junto al del Juvenil A de 10:00 a 12:00
		assertEquals("", asignar(CAMPO, LUNES_2_NOV, 540, 600));
		assertEquals("", asignar(CAMPO, LUNES_2_NOV, 720, 780));
		assertEquals("JARDINERIA 480-540 ENTRENAMIENTO 540-600 ENTRENAMIENTO 600-720 ENTRENAMIENTO 720-780"
				+ " RESERVA 1080-1170", usos(CAMPO, LUNES_2_NOV));
	}

	@Test
	void entrenamientoPegadoAUnPartido() {
		assertEquals("", asignar(ESTADIO, DOMINGO_8_NOV, 1140, 1260));
		assertEquals("", asignar(ESTADIO, DOMINGO_8_NOV, 900, 1020));
	}

	@Test
	void entrenamientoDeMadrugadaYHastaMedianoche() {
		// Los entrenamientos no estan limitados al horario de las reservas externas (8:00 a 22:00)
		assertEquals("", asignar(CAMPO, MARTES_3_NOV, 0, 60));
		assertEquals("", asignar(CAMPO, MARTES_3_NOV, 1380, 1440));
	}

	@Test
	void entrenamientoParaHoy() {
		assertEquals("", asignar(CAMPO, HOY.toString(), 600, 660));
	}

	@Test
	void otraInstalacionNoAfecta() {
		// A la hora de la reserva y el entrenamiento del campo, pero en el estadio
		assertEquals("", asignar(ESTADIO, LUNES_2_NOV, 600, 1140));
		assertEquals(CAMPO_LUNES, usos(CAMPO, LUNES_2_NOV));
	}

	// ---- Coincide con un partido u otro entrenamiento ----

	@ParameterizedTest
	@CsvSource({ "600, 720", // la misma franja
			"660, 780", // empieza durante el otro entrenamiento
			"540, 630", // acaba durante el otro entrenamiento
			"630, 690", // dentro del otro entrenamiento
			"540, 780" // lo contiene
	})
	void coincideConOtroEntrenamiento(int inicio, int fin) {
		assertError("No se puede asignar el entrenamiento: coincide con el entrenamiento del Juvenil A de 10:00 a 12:00"
				+ " en esta instalación", crear(CAMPO, LUNES_2_NOV, inicio, fin));
	}

	@Test
	void coincideConUnPartido() {
		assertError("No se puede asignar el entrenamiento: coincide con el partido del Primer equipo de 17:00 a 19:00"
				+ " en esta instalación", crear(ESTADIO, DOMINGO_8_NOV, 1080, 1200));
	}

	@Test
	void coincideConEntrenamientoYReservaNoEliminaNada() {
		// Aunque tambien coincida con la reserva y la labor, no se asigna y no se elimina nada
		assertError("No se puede asignar el entrenamiento: coincide con el entrenamiento del Juvenil A de 10:00 a 12:00"
				+ " en esta instalación", crear(CAMPO, LUNES_2_NOV, 480, 1200));
	}

	@Test
	void coincideConElNuevoEntrenamiento() {
		asignar(CAMPO, MARTES_3_NOV, 600, 720);
		assertError("No se puede asignar el entrenamiento: coincide con el entrenamiento del Primer equipo de 10:00"
				+ " a 12:00 en esta instalación", crear(CAMPO, MARTES_3_NOV, 700, 800));
	}

	// ---- Reservas externas: se eliminan si coinciden o empiezan en la hora y media siguiente ----

	@Test
	void eliminaReservaQueCoincideEnParte() {
		// El ejemplo del mockup: 17:00-18:30 pisa la reserva de 18:00 a 19:30
		assertEquals("RESERVA 1080-1170", asignar(CAMPO, LUNES_2_NOV, 1020, 1110));
		assertEquals("JARDINERIA 480-540 ENTRENAMIENTO 600-720 ENTRENAMIENTO 1020-1110", usos(CAMPO, LUNES_2_NOV));
	}

	@Test
	void eliminaReservaQueContieneAlEntrenamiento() {
		assertEquals("RESERVA 1080-1170", asignar(CAMPO, LUNES_2_NOV, 1100, 1120));
	}

	@Test
	void eliminaReservaQueEmpiezaAlAcabarElEntrenamiento() {
		assertEquals("RESERVA 1080-1170", asignar(CAMPO, LUNES_2_NOV, 960, 1080));
	}

	@Test
	void eliminaReservaQueEmpiezaElUltimoMinutoDelMargen() {
		// Acaba a las 16:31: el margen llega hasta las 18:01 y la reserva empieza a las 18:00
		assertEquals("RESERVA 1080-1170", asignar(CAMPO, LUNES_2_NOV, 900, 991));
	}

	@Test
	void mantieneReservaQueEmpiezaAlAcabarElMargen() {
		// Acaba a las 16:30: la reserva de las 18:00 empieza justo al pasar la hora y media
		assertEquals("", asignar(CAMPO, LUNES_2_NOV, 900, 990));
		assertEquals("JARDINERIA 480-540 ENTRENAMIENTO 600-720 ENTRENAMIENTO 900-990 RESERVA 1080-1170",
				usos(CAMPO, LUNES_2_NOV));
	}

	@Test
	void mantieneReservaQueAcabaAlEmpezarElEntrenamiento() {
		assertEquals("", asignar(CAMPO, LUNES_2_NOV, 1170, 1260));
	}

	@Test
	void eliminaVariasReservas() {
		db.executeUpdate("insert into reserva(instalacionId, fecha, horaInicio, horaFin, nombreUsuario, numeroTarjeta,"
				+ " importe) values (?, ?, ?, ?, ?, ?, ?)", idDe("instalacion", CAMPO), MARTES_3_NOV, 600, 660, "A",
				"4111", 50);
		db.executeUpdate("insert into reserva(instalacionId, fecha, horaInicio, horaFin, nombreUsuario, numeroTarjeta,"
				+ " importe) values (?, ?, ?, ?, ?, ?, ?)", idDe("instalacion", CAMPO), MARTES_3_NOV, 750, 810, "B",
				"4111", 50);
		db.executeUpdate("insert into reserva(instalacionId, fecha, horaInicio, horaFin, nombreUsuario, numeroTarjeta,"
				+ " importe) values (?, ?, ?, ?, ?, ?, ?)", idDe("instalacion", CAMPO), MARTES_3_NOV, 900, 960, "C",
				"4111", 50);
		// 10:30-12:00: pisa la de 10:00, la de 12:30 empieza en el margen y la de 15:00 queda fuera
		assertEquals("RESERVA 600-660 RESERVA 750-810", asignar(CAMPO, MARTES_3_NOV, 630, 720));
		assertEquals("ENTRENAMIENTO 630-720 RESERVA 900-960", usos(CAMPO, MARTES_3_NOV));
	}

	// ---- Labores de jardineria: se eliminan solo si coinciden, aunque sea en parte ----

	@Test
	void eliminaLaborQueCoincideEnParte() {
		assertEquals("JARDINERIA 480-540", asignar(CAMPO, LUNES_2_NOV, 510, 570));
		assertEquals("ENTRENAMIENTO 510-570 ENTRENAMIENTO 600-720 RESERVA 1080-1170", usos(CAMPO, LUNES_2_NOV));
	}

	@Test
	void eliminaLaborContenidaEnElEntrenamiento() {
		assertEquals("JARDINERIA 540-660", asignar(ESTADIO, MARTES_3_NOV, 480, 720));
	}

	@Test
	void mantieneLaborPegada() {
		assertEquals("", asignar(CAMPO, LUNES_2_NOV, 420, 480));
		assertEquals("", asignar(ESTADIO, MARTES_3_NOV, 660, 720));
	}

	@Test
	void mantieneLaborEnLaHoraYMediaSiguiente() {
		// El margen de hora y media solo afecta a las reservas externas, no a la jardineria
		assertEquals("", asignar(CAMPO, LUNES_2_NOV, 390, 450));
		assertEquals("ENTRENAMIENTO 390-450 " + CAMPO_LUNES, usos(CAMPO, LUNES_2_NOV));
	}

	@Test
	void eliminaLaborYReservaALaVez() {
		db.executeUpdate("insert into labor_jardineria(instalacionId, empleadoId, descripcion, fecha, horaInicio, horaFin)"
				+ " select ?, id, 'Abonar', ?, 960, 1020 from empleado where dni = '88888888Y'", idDe("instalacion", CAMPO),
				LUNES_2_NOV);
		assertEquals("JARDINERIA 960-1020 RESERVA 1080-1170", asignar(CAMPO, LUNES_2_NOV, 900, 1050));
	}

	// ---- Previsualizacion ----

	@Test
	void previsualizarNoEliminaNada() {
		assertEquals("RESERVA 1080-1170", resumir(model.getAfectados(crear(CAMPO, LUNES_2_NOV, 1020, 1110))));
		assertEquals(CAMPO_LUNES, usos(CAMPO, LUNES_2_NOV));
	}

	@Test
	void usosDelDiaConSusDescripciones() {
		List<UsoInstalacionDTO> usos = model.getUsos(idDe("instalacion", CAMPO), LocalDate.parse(LUNES_2_NOV));
		assertEquals("la labor de jardinería «Cortar el césped» de 08:00 a 09:00", usos.get(0).describir());
		assertEquals("el entrenamiento del Juvenil A de 10:00 a 12:00", usos.get(1).describir());
		assertEquals("la reserva externa de Club de pádel Oviedo de 18:00 a 19:30", usos.get(2).describir());
	}

	@Test
	void trasAsignarNoSePuedeReservarEnElMargen() {
		// Relacion con la HU 35206: el nuevo entrenamiento bloquea las reservas externas hasta hora y media despues
		asignar(CAMPO, MARTES_3_NOV, 600, 720);
		StringBuilder sb = new StringBuilder();
		for (FranjaDTO f : new ReservaModel(HOY).getFranjasLibres(idDe("instalacion", CAMPO),
				LocalDate.parse(MARTES_3_NOV)))
			sb.append(sb.length() == 0 ? "" : " ").append(f.getHoraInicio()).append("-").append(f.getHoraFin());
		assertEquals("480-600 810-1320", sb.toString());
	}

	// ---- Datos invalidos ----

	@Test
	void fechaPasada() {
		assertError("No se puede asignar un entrenamiento en una fecha pasada", crear(CAMPO, "2026-10-28", 600, 660));
	}

	@ParameterizedTest
	@CsvSource({ "660, 660", "660, 600" })
	void finNoPosteriorAlInicio(int inicio, int fin) {
		assertError("La hora de fin debe ser posterior a la de inicio", crear(CAMPO, MARTES_3_NOV, inicio, fin));
	}

	@ParameterizedTest
	@CsvSource({ "-1, 60", "1380, 1441" })
	void horasFueraDelDia(int inicio, int fin) {
		assertError("Las horas deben estar entre las 00:00 y las 24:00", crear(CAMPO, MARTES_3_NOV, inicio, fin));
	}

	@Test
	void equipoInexistente() {
		EntrenamientoEntity e = crear(CAMPO, MARTES_3_NOV, 600, 660);
		e.setEquipoId(-1);
		assertError("No existe el equipo seleccionado", e);
	}

	@Test
	void instalacionInexistente() {
		EntrenamientoEntity e = crear(CAMPO, MARTES_3_NOV, 600, 660);
		e.setInstalacionId(-1);
		assertError("No existe la instalación seleccionada", e);
	}

	@Test
	void sinFecha() {
		assertError("Hay que indicar la fecha del entrenamiento", crear(CAMPO, "", 600, 660));
	}
}
