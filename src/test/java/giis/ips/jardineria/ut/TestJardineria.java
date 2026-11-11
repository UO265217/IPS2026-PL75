package giis.ips.jardineria.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.entrenamientos.UsoInstalacionDTO;
import giis.ips.jardineria.JardineriaModel;
import giis.ips.jardineria.JardineroDTO;
import giis.ips.jardineria.LaborJardineriaEntity;
import giis.ips.jardineria.LaborJardineroDTO;
import giis.ips.reservas.FranjaDTO;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 36125 (programar labores de jardineria) sobre el modelo y la base de datos.
 * Parten de los datos de ejemplo de data.sql y fijan como dia actual el jueves 29/10/2026:
 * - Pedro Sanz (jardinero): de lunes a sabado de 7:00 a 13:00; el domingo 25/10, solo de 8:00 a 12:00 (dia especifico).
 * - Marcos Diaz (jardinero): de lunes a viernes de 12:00 a 20:00.
 * - Campo de entrenamiento 1, lunes 2/11: labor de Pedro 8:00-9:00 (Cortar el cesped), entrenamiento del Juvenil A
 *   10:00-12:00 y reserva externa del Club de padel Oviedo 18:00-19:30.
 * - Estadio, lunes 2/11: labor de Pedro 9:00-10:00 (Abonar el cesped).
 * Las franjas se resumen como "inicio-fin" (minutos desde las 00:00) para compararlas de una vez.
 */
class TestJardineria {
	private static final LocalDate HOY = LocalDate.parse("2026-10-29");
	private static final String CAMPO = "Campo de entrenamiento 1";
	private static final String ESTADIO = "Estadio";
	private static final String PEDRO = "88888888Y";
	private static final String MARCOS = "23456789D";
	private static final String ANA = "11111111H";
	private static final String LUNES_2_NOV = "2026-11-02";
	private static final String MARTES_3_NOV = "2026-11-03";
	private static final String DOMINGO_8_NOV = "2026-11-08";
	private static final String DOMINGO_25_OCT = "2026-10-25";

	private Database db = new Database();
	private JardineriaModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new JardineriaModel(HOY);
	}

	private int instalacion(String nombre) {
		return (Integer) db.executeQueryArray("select id from instalacion where nombre = ?", nombre).get(0)[0];
	}

	private int empleado(String dni) {
		return (Integer) db.executeQueryArray("select id from empleado where dni = ?", dni).get(0)[0];
	}

	private LaborJardineriaEntity crear(String dni, String instalacion, String fecha, int inicio, int fin) {
		LaborJardineriaEntity l = new LaborJardineriaEntity();
		l.setEmpleadoId(empleado(dni));
		l.setInstalacionId(instalacion(instalacion));
		l.setDescripcion("Podar los setos");
		l.setFecha(fecha);
		l.setHoraInicio(inicio);
		l.setHoraFin(fin);
		return l;
	}

	private void programar(String dni, String instalacion, String fecha, int inicio, int fin) {
		model.programar(crear(dni, instalacion, fecha, inicio, fin));
	}

	private int numLabores() {
		return db.executeQueryArray("select id from labor_jardineria").size();
	}

	/** Comprueba el mensaje y que no se ha guardado nada */
	private void assertError(String mensaje, LaborJardineriaEntity l) {
		int antes = numLabores();
		ApplicationException ex = assertThrows(ApplicationException.class, () -> model.programar(l));
		assertEquals(mensaje, ex.getMessage());
		assertEquals(antes, numLabores(), "Si hay error no se debe guardar nada");
	}

	private static String franjas(List<FranjaDTO> franjas) {
		StringBuilder sb = new StringBuilder();
		for (FranjaDTO f : franjas)
			sb.append(sb.length() == 0 ? "" : " ").append(f.getHoraInicio()).append("-").append(f.getHoraFin());
		return sb.toString();
	}

	private String usos(String instalacion, String fecha, String dni) {
		StringBuilder sb = new StringBuilder();
		for (UsoInstalacionDTO u : model.getUsosInstalacion(instalacion(instalacion), LocalDate.parse(fecha),
				empleado(dni)))
			sb.append(sb.length() == 0 ? "" : " ").append(u.getTipo()).append(" ").append(u.getHoraInicio())
					.append("-").append(u.getHoraFin());
		return sb.toString();
	}

	// ---- Consultas para la previsualizacion ----

	@Test
	void jardinerosOrdenadosPorApellido() {
		StringBuilder sb = new StringBuilder();
		for (JardineroDTO j : model.getJardineros())
			sb.append(sb.length() == 0 ? "" : ", ").append(j.getNombreCompleto());
		assertEquals("Marcos Díaz, Pedro Sanz", sb.toString());
	}

	@Test
	void turnosDelHorarioPeriodico() {
		assertEquals("420-780", franjas(model.getTurnos(empleado(PEDRO), LocalDate.parse(LUNES_2_NOV))));
		assertEquals("720-1200", franjas(model.getTurnos(empleado(MARCOS), LocalDate.parse(LUNES_2_NOV))));
		assertEquals("", franjas(model.getTurnos(empleado(PEDRO), LocalDate.parse(DOMINGO_8_NOV))));
	}

	@Test
	void elHorarioEspecificoSustituyeAlPeriodico() {
		assertEquals("480-720", franjas(model.getTurnos(empleado(PEDRO), LocalDate.parse(DOMINGO_25_OCT))));
	}

	@Test
	void usosDeLaInstalacionSinLasLaboresDelJardinero() {
		assertEquals("ENTRENAMIENTO 600-720 RESERVA 1080-1170", usos(CAMPO, LUNES_2_NOV, PEDRO));
		assertEquals("JARDINERIA 480-540 ENTRENAMIENTO 600-720 RESERVA 1080-1170", usos(CAMPO, LUNES_2_NOV, MARCOS));
	}

	@Test
	void laboresDelJardineroEnTodasLasInstalaciones() {
		StringBuilder sb = new StringBuilder();
		for (LaborJardineroDTO l : model.getLaboresJardinero(empleado(PEDRO), LocalDate.parse(LUNES_2_NOV)))
			sb.append(sb.length() == 0 ? "" : ", ").append(l.getInstalacion()).append(" ").append(l.getHoraInicio())
					.append("-").append(l.getHoraFin());
		assertEquals(CAMPO + " 480-540, " + ESTADIO + " 540-600", sb.toString());
	}

	// ---- Labores que se pueden programar ----

	@Test
	void programarGuardaTodosLosDatos() {
		LaborJardineriaEntity l = crear(PEDRO, CAMPO, LUNES_2_NOV, 720, 780);
		l.setDescripcion("  Podar los setos  ");
		model.programar(l);
		List<Object[]> filas = db.executeQueryArray("select instalacionId, empleadoId, descripcion, horaInicio, horaFin"
				+ " from labor_jardineria where fecha = ? and horaInicio = 720", LUNES_2_NOV);
		assertEquals(1, filas.size());
		assertEquals(instalacion(CAMPO), filas.get(0)[0]);
		assertEquals(empleado(PEDRO), filas.get(0)[1]);
		assertEquals("Podar los setos", filas.get(0)[2]);
		assertEquals(720, filas.get(0)[3]);
		assertEquals(780, filas.get(0)[4]);
	}

	@Test
	void laborPegadaAUnEntrenamientoYAOtraLaborSuya() {
		// 12:00-13:00 empieza cuando acaba el entrenamiento; 10:00-11:00 cuando acaba su labor en el estadio
		programar(PEDRO, CAMPO, LUNES_2_NOV, 720, 780);
		programar(PEDRO, ESTADIO, LUNES_2_NOV, 600, 660);
		assertEquals(5, numLabores());
	}

	@Test
	void laborQueOcupaTodoElTurno() {
		// Sabado 7/11: Pedro trabaja de 7:00 a 13:00 y no tiene labores
		programar(PEDRO, CAMPO, "2026-11-07", 420, 780);
		assertEquals(4, numLabores());
	}

	@Test
	void laborEnUnDiaConHorarioEspecifico() {
		model = new JardineriaModel(LocalDate.parse("2026-10-20"));
		programar(PEDRO, CAMPO, DOMINGO_25_OCT, 480, 540);
		assertEquals(4, numLabores());
	}

	@Test
	void dosJardinerosALaVezEnDistintasInstalaciones() {
		programar(PEDRO, CAMPO, LUNES_2_NOV, 720, 780);
		programar(MARCOS, ESTADIO, LUNES_2_NOV, 720, 780);
		assertEquals(5, numLabores());
	}

	@Test
	void laborParaHoy() {
		programar(PEDRO, CAMPO, HOY.toString(), 600, 660);
		assertEquals(4, numLabores());
	}

	// ---- Labores que no se pueden programar ----

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			// Horas
			"88888888Y | 600 | 600 | La hora de fin debe ser posterior a la de inicio",
			"88888888Y | 660 | 600 | La hora de fin debe ser posterior a la de inicio",
			"88888888Y | 1380 | 1500 | Las horas deben estar entre las 00:00 y las 24:00",
			// Fuera del turno de Pedro (7:00 a 13:00), aunque sea en parte
			"88888888Y | 750 | 840 | La labor debe quedar dentro de un turno de Pedro Sanz (este día, de 07:00 a 13:00)",
			"88888888Y | 360 | 450 | La labor debe quedar dentro de un turno de Pedro Sanz (este día, de 07:00 a 13:00)",
			// Coincide con otra labor suya, en esta instalacion o en otra (se comprueba antes que la instalacion)
			"88888888Y | 450 | 510 | Pedro Sanz ya tiene otra labor de 08:00 a 09:00 (Cortar el césped, Campo de entrenamiento 1)",
			"88888888Y | 570 | 630 | Pedro Sanz ya tiene otra labor de 09:00 a 10:00 (Abonar el césped, Estadio)",
			// Coincide con un uso de la instalacion
			"88888888Y | 660 | 720 | Coincide con el entrenamiento del Juvenil A de 10:00 a 12:00 en esta instalación",
			"23456789D | 1050 | 1110 | Coincide con la reserva externa de Club de pádel Oviedo de 18:00 a 19:30 en esta instalación",
			"23456789D | 690 | 750 | La labor debe quedar dentro de un turno de Marcos Díaz (este día, de 12:00 a 20:00)", })
	void laborQueNoCumpleLasReglas(String dni, int inicio, int fin, String mensaje) {
		assertError(mensaje, crear(dni, CAMPO, LUNES_2_NOV, inicio, fin));
	}

	@Test
	void coincideConLaLaborDeOtroJardinero() {
		programar(PEDRO, CAMPO, LUNES_2_NOV, 720, 780);
		assertError("Coincide con la labor de jardinería «Podar los setos» de 12:00 a 13:00 en esta instalación",
				crear(MARCOS, CAMPO, LUNES_2_NOV, 750, 810));
	}

	@Test
	void jardineroQueNoTrabajaEseDia() {
		assertError("Pedro Sanz no trabaja este día", crear(PEDRO, CAMPO, DOMINGO_8_NOV, 600, 660));
	}

	@Test
	void fechaPasada() {
		assertError("No se puede programar una labor en una fecha pasada", crear(PEDRO, CAMPO, "2026-10-28", 600, 660));
	}

	@Test
	void descripcionVacia() {
		LaborJardineriaEntity l = crear(PEDRO, CAMPO, MARTES_3_NOV, 600, 660);
		l.setDescripcion("   ");
		assertError("Hay que indicar la descripción de la labor", l);
		l.setDescripcion(null);
		assertError("Hay que indicar la descripción de la labor", l);
	}

	@Test
	void empleadoQueNoEsJardinero() {
		assertError("El empleado seleccionado no es jardinero", crear(ANA, CAMPO, MARTES_3_NOV, 600, 660));
	}

	@Test
	void jardineroOInstalacionInexistentes() {
		LaborJardineriaEntity l = crear(PEDRO, CAMPO, MARTES_3_NOV, 600, 660);
		l.setEmpleadoId(999);
		assertError("No existe el jardinero seleccionado", l);
		l.setEmpleadoId(empleado(PEDRO));
		l.setInstalacionId(999);
		assertError("No existe la instalación seleccionada", l);
	}
}
