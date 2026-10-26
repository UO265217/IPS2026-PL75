package giis.ips.empleados.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.empleados.HorarioEntity;
import giis.ips.empleados.HorarioEspecificoController;
import giis.ips.empleados.HorarioEspecificoEntity;
import giis.ips.empleados.HorarioModel;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 35204 (horarios de dia especifico de empleados no deportivos): recorren el flujo completo
 * controlador (validacion sintactica) -> modelo (reglas de negocio) -> base de datos.
 * Parten de los datos de ejemplo de data.sql y usan sobre todo la semana del 02/11/2026 al 08/11/2026,
 * que no tiene dias especificos: Ana 27 h (lunes 7 h, de martes a viernes 5 h), Carlos 40 h (8 h de lunes
 * a viernes), Pedro 36 h (6 h de lunes a sabado), Sara sin horario y Luis es deportivo.
 * Cada turno de la semana se resume como "dia inicio-fin" (minutos) y con un asterisco si es de dia especifico.
 */
class TestHorarioEspecifico {
	private static final String ANA = "11111111H";
	private static final String LUIS = "22222222J";
	private static final String CARLOS = "66666666Z";
	private static final String PEDRO = "88888888Y";
	private static final String SARA = "99999999R";
	private static final String LUNES_2_NOV = "2026-11-02";
	private static final String MARTES_3_NOV = "2026-11-03";
	private static final String SABADO_7_NOV = "2026-11-07";
	private static final String DOMINGO_8_NOV = "2026-11-08";

	private Database db = new Database();
	private HorarioModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new HorarioModel();
	}

	private int idDe(String dni) {
		return (Integer) db.executeQueryArray("select id from empleado where dni = ?", dni).get(0)[0];
	}

	/** Ejecuta el flujo completo con los datos tal y como se escribirian en el formulario */
	private void anyadir(String dni, String fecha, String inicio, String fin) {
		model.anyadirTurnoEspecifico(HorarioEspecificoController.crearTurno(idDe(dni), fecha, inicio, fin));
	}

	private int contarEspecificos() {
		return db.executeQueryArray("select id from horario_especifico").size();
	}

	/** Comprueba el mensaje y que no se ha guardado ni borrado ningun turno especifico */
	private void assertError(String mensaje, Runnable accion) {
		int antes = contarEspecificos();
		ApplicationException ex = assertThrows(ApplicationException.class, accion::run);
		assertEquals(mensaje, ex.getMessage());
		assertEquals(antes, contarEspecificos(), "Si hay error no se debe guardar ni borrar nada");
	}

	/** Turnos de la semana resumidos en texto para compararlos de una vez */
	private String semana(String dni, String fecha) {
		StringBuilder sb = new StringBuilder();
		for (HorarioEntity h : model.getHorarioSemana(idDe(dni), LocalDate.parse(fecha)))
			sb.append(h.getDiaSemana()).append(" ").append(h.getHoraInicio()).append("-").append(h.getHoraFin())
					.append(h.isEspecifico() ? "*" : "").append("\n");
		return sb.toString();
	}

	private int idEspecifico(String dni, String fecha, int horaInicio) {
		for (HorarioEspecificoEntity h : model.getHorariosEspecificos(idDe(dni), LocalDate.parse(fecha)))
			if (h.getHoraInicio() == horaInicio)
				return h.getId();
		throw new AssertionError("No existe el turno especifico");
	}

	// ---- Turnos validos ----

	@Test
	void anyadirUnTurno() {
		anyadir(ANA, MARTES_3_NOV, "10:30", "13:15");
		List<HorarioEspecificoEntity> turnos = model.getHorariosEspecificos(idDe(ANA), LocalDate.parse(MARTES_3_NOV));
		assertEquals(1, turnos.size());
		assertEquals(MARTES_3_NOV, turnos.get(0).getFecha());
		assertEquals(10 * 60 + 30, turnos.get(0).getHoraInicio());
		assertEquals(13 * 60 + 15, turnos.get(0).getHoraFin());
		assertEquals(idDe(ANA), turnos.get(0).getEmpleadoId());
	}

	@Test
	void elPrimerTurnoSustituyeTodoElPeriodicoDelDia() {
		// El lunes Ana tiene 9:00-14:00 y 16:00-18:00; el turno especifico no se solapa con ellos porque los sustituye
		anyadir(ANA, LUNES_2_NOV, "10:00", "12:00");
		assertEquals("1 600-720*\n2 540-840\n3 540-840\n4 540-840\n5 540-840\n", semana(ANA, LUNES_2_NOV));
	}

	@Test
	void variosTurnosEnElMismoDiaOrdenadosPorHora() {
		anyadir(ANA, MARTES_3_NOV, "16:00", "19:00");
		anyadir(ANA, MARTES_3_NOV, "08:00", "12:00");
		List<HorarioEspecificoEntity> turnos = model.getHorariosEspecificos(idDe(ANA), LocalDate.parse(MARTES_3_NOV));
		assertEquals(2, turnos.size());
		assertEquals(8 * 60, turnos.get(0).getHoraInicio());
		assertEquals(16 * 60, turnos.get(1).getHoraInicio());
	}

	@Test
	void turnosPegadosNoSeSolapan() {
		anyadir(ANA, MARTES_3_NOV, "09:00", "13:00");
		anyadir(ANA, MARTES_3_NOV, "13:00", "17:00");
		assertEquals(2, model.getHorariosEspecificos(idDe(ANA), LocalDate.parse(MARTES_3_NOV)).size());
	}

	@Test
	void turnoEnUnDiaSinHorarioPeriodico() {
		anyadir(ANA, SABADO_7_NOV, "10:00", "14:00");
		assertEquals(true, semana(ANA, SABADO_7_NOV).contains("6 600-840*\n"));
	}

	@Test
	void soloAfectaASuFecha() {
		// El martes de la semana siguiente Ana vuelve a su horario periodico
		anyadir(ANA, MARTES_3_NOV, "10:00", "12:00");
		assertEquals(true, semana(ANA, "2026-11-10").contains("2 540-840\n"));
	}

	@Test
	void turnoHastaMedianoche() {
		anyadir(ANA, MARTES_3_NOV, "18:00", "24:00");
		assertEquals(24 * 60, model.getHorariosEspecificos(idDe(ANA), LocalDate.parse(MARTES_3_NOV)).get(0).getHoraFin());
	}

	// ---- Limite diario de 8 h ----

	@Test
	void ochoHorasEnElDiaEsElLimite() {
		anyadir(ANA, MARTES_3_NOV, "08:00", "12:00");
		anyadir(ANA, MARTES_3_NOV, "14:00", "18:00");
		assertEquals(2, model.getHorariosEspecificos(idDe(ANA), LocalDate.parse(MARTES_3_NOV)).size());
	}

	@Test
	void masDeOchoHorasEnUnTurno() {
		assertError("No se pueden superar las 8 horas de trabajo diarias",
				() -> anyadir(ANA, MARTES_3_NOV, "08:00", "16:01"));
	}

	@Test
	void masDeOchoHorasSumandoLosTurnosDelDia() {
		anyadir(ANA, MARTES_3_NOV, "08:00", "12:00");
		anyadir(ANA, MARTES_3_NOV, "14:00", "17:00");
		assertError("No se pueden superar las 8 horas de trabajo diarias",
				() -> anyadir(ANA, MARTES_3_NOV, "18:00", "19:01"));
	}

	@Test
	void elLimiteDiarioNoCuentaElPeriodicoSustituido() {
		// Carlos tiene 8 h periodicas el martes, pero el dia especifico parte de cero
		anyadir(CARLOS, MARTES_3_NOV, "06:00", "14:00");
		assertEquals(true, semana(CARLOS, MARTES_3_NOV).contains("2 360-840*\n"));
	}

	// ---- Limite semanal de 40 h ----

	@Test
	void sustituirUnDiaSinCambiarLasHorasDeLaSemana() {
		// Carlos ya tiene 40 h: cambiar las 8 h del martes por otras 8 h sigue en 40 h
		anyadir(CARLOS, MARTES_3_NOV, "12:00", "20:00");
		assertEquals(1, model.getHorariosEspecificos(idDe(CARLOS), LocalDate.parse(MARTES_3_NOV)).size());
	}

	@Test
	void masDeCuarentaHorasEnLaSemana() {
		// Carlos tiene 40 h de lunes a viernes: no puede trabajar ademas el sabado
		assertError("No se pueden superar las 40 horas de trabajo semanales",
				() -> anyadir(CARLOS, SABADO_7_NOV, "10:00", "10:01"));
	}

	@Test
	void cuarentaHorasEnLaSemanaEsElLimite() {
		// Pedro tiene 36 h: el domingo puede trabajar justo 4 h
		anyadir(PEDRO, DOMINGO_8_NOV, "08:00", "12:00");
		assertEquals(40 * 60, totalMinutos(PEDRO, DOMINGO_8_NOV));
	}

	@Test
	void elLimiteSemanalCuentaLosOtrosDiasEspecificos() {
		// Con el domingo especifico Pedro llega a 40 h; el sabado especifico de 7 h sustituye a 6 h y pasaria a 41 h
		anyadir(PEDRO, DOMINGO_8_NOV, "08:00", "12:00");
		assertError("No se pueden superar las 40 horas de trabajo semanales",
				() -> anyadir(PEDRO, SABADO_7_NOV, "07:00", "14:00"));
	}

	@Test
	void elLimiteSemanalCuentaLosTurnosEspecificosDelMismoDia() {
		// Pedro: el domingo 4 h en dos turnos llega a 40 h; un tercer turno las supera
		anyadir(PEDRO, DOMINGO_8_NOV, "08:00", "10:00");
		anyadir(PEDRO, DOMINGO_8_NOV, "11:00", "13:00");
		assertError("No se pueden superar las 40 horas de trabajo semanales",
				() -> anyadir(PEDRO, DOMINGO_8_NOV, "14:00", "14:30"));
	}

	@Test
	void elLimiteSemanalSoloCuentaSuSemana() {
		// Los dias especificos de la semana anterior no cuentan para la siguiente
		anyadir(PEDRO, DOMINGO_8_NOV, "08:00", "12:00");
		anyadir(PEDRO, "2026-11-15", "08:00", "12:00");
		assertEquals(40 * 60, totalMinutos(PEDRO, "2026-11-15"));
	}

	@Test
	void elLimiteSemanalCuentaLaSemanaDeLunesADomingo() {
		// El domingo 25/10 de data.sql deja a Pedro en 40 h en su semana; el lunes 26/10 ya es otra semana
		assertError("No se pueden superar las 40 horas de trabajo semanales",
				() -> anyadir(PEDRO, "2026-10-24", "07:00", "13:30"));
		anyadir(PEDRO, "2026-10-26", "07:00", "13:30");
	}

	private int totalMinutos(String dni, String fecha) {
		int total = 0;
		for (HorarioEntity h : model.getHorarioSemana(idDe(dni), LocalDate.parse(fecha)))
			total += h.getHoraFin() - h.getHoraInicio();
		return total;
	}

	// ---- Otras reglas de negocio ----

	@Test
	void solapeConOtroTurnoEspecificoDelDia() {
		anyadir(ANA, MARTES_3_NOV, "09:00", "13:00");
		assertError("El turno se solapa con otro turno del mismo día",
				() -> anyadir(ANA, MARTES_3_NOV, "12:59", "15:00"));
	}

	@Test
	void turnoQueContieneAOtro() {
		anyadir(ANA, MARTES_3_NOV, "10:00", "11:00");
		assertError("El turno se solapa con otro turno del mismo día",
				() -> anyadir(ANA, MARTES_3_NOV, "09:00", "12:00"));
	}

	@Test
	void empleadoSinHorarioPeriodico() {
		assertError("El empleado no tiene horario periódico: hay que asignárselo antes de añadir días específicos",
				() -> anyadir(SARA, MARTES_3_NOV, "09:00", "13:00"));
	}

	@Test
	void empleadoDeportivo() {
		assertError("Solo se puede asignar horario a empleados no deportivos",
				() -> anyadir(LUIS, MARTES_3_NOV, "09:00", "13:00"));
	}

	@Test
	void empleadoInexistente() {
		assertError("No existe el empleado seleccionado", () -> model.anyadirTurnoEspecifico(
				HorarioEspecificoController.crearTurno(999, MARTES_3_NOV, "09:00", "13:00")));
	}

	@ParameterizedTest
	@CsvSource({ "13:00, 09:00", "09:00, 09:00" })
	void inicioNoAnteriorAlFin(String inicio, String fin) {
		assertError("La hora de inicio debe ser anterior a la hora de fin",
				() -> anyadir(ANA, MARTES_3_NOV, inicio, fin));
	}

	// ---- Validaciones sintacticas del controlador ----

	@ParameterizedTest
	@CsvSource(value = { "NULL", "''", "'  '" }, nullValues = "NULL")
	void fechaVacia(String fecha) {
		assertError("Hay que seleccionar el día", () -> anyadir(ANA, fecha, "09:00", "13:00"));
	}

	@ParameterizedTest
	@CsvSource({ "03/11/2026", "2026-11-31", "ayer" })
	void fechaConFormatoIncorrecto(String fecha) {
		assertError("La fecha debe tener el formato aaaa-mm-dd", () -> anyadir(ANA, fecha, "09:00", "13:00"));
	}

	@Test
	void horaVacia() {
		assertError("Hay que indicar la hora de inicio", () -> anyadir(ANA, MARTES_3_NOV, "", "13:00"));
		assertError("Hay que indicar la hora de fin", () -> anyadir(ANA, MARTES_3_NOV, "09:00", null));
	}

	@ParameterizedTest
	@CsvSource({ "9:00", "24:01", "10:60" })
	void horaConFormatoIncorrecto(String hora) {
		assertError("La hora debe tener el formato HH:mm (entre 00:00 y 24:00)",
				() -> anyadir(ANA, MARTES_3_NOV, hora, "23:00"));
	}

	// ---- Eliminar turnos ----

	@Test
	void eliminarUnTurno() {
		anyadir(ANA, MARTES_3_NOV, "08:00", "10:00");
		anyadir(ANA, MARTES_3_NOV, "11:00", "13:00");
		model.eliminarTurnoEspecifico(idEspecifico(ANA, MARTES_3_NOV, 8 * 60));
		assertEquals(true, semana(ANA, MARTES_3_NOV).contains("2 660-780*\n3 540-840\n"));
		assertEquals(false, semana(ANA, MARTES_3_NOV).contains("2 480-600*"));
	}

	@Test
	void eliminarElUltimoTurnoVuelveAlPeriodico() {
		anyadir(ANA, LUNES_2_NOV, "10:00", "12:00");
		model.eliminarTurnoEspecifico(idEspecifico(ANA, LUNES_2_NOV, 10 * 60));
		assertEquals("1 540-840\n1 960-1080\n2 540-840\n3 540-840\n4 540-840\n5 540-840\n", semana(ANA, LUNES_2_NOV));
	}

	@Test
	void eliminarElUltimoTurnoSiSuperariaLasCuarentaHoras() {
		// Pedro: el lunes pasa de 6 h a 2 h (32 h) y el domingo trabaja 8 h (40 h).
		// Si se quita el lunes especifico, el lunes vuelve a sus 6 h y la semana pasaria a 44 h
		anyadir(PEDRO, LUNES_2_NOV, "08:00", "10:00");
		anyadir(PEDRO, DOMINGO_8_NOV, "08:00", "16:00");
		assertError("No se puede eliminar: el día volvería a su horario periódico y se superarían las 40 horas semanales",
				() -> model.eliminarTurnoEspecifico(idEspecifico(PEDRO, LUNES_2_NOV, 8 * 60)));
		// Quitando antes el domingo si se puede
		model.eliminarTurnoEspecifico(idEspecifico(PEDRO, DOMINGO_8_NOV, 8 * 60));
		model.eliminarTurnoEspecifico(idEspecifico(PEDRO, LUNES_2_NOV, 8 * 60));
		assertEquals(36 * 60, totalMinutos(PEDRO, LUNES_2_NOV));
	}

	@Test
	void eliminarUnTurnoNoUltimoNoCompruebaElPeriodico() {
		// Con dos turnos el lunes, quitar uno deja el dia como especifico y la semana baja
		anyadir(PEDRO, LUNES_2_NOV, "08:00", "09:00");
		anyadir(PEDRO, LUNES_2_NOV, "10:00", "11:00");
		anyadir(PEDRO, DOMINGO_8_NOV, "08:00", "16:00");
		model.eliminarTurnoEspecifico(idEspecifico(PEDRO, LUNES_2_NOV, 10 * 60));
		assertEquals(39 * 60, totalMinutos(PEDRO, LUNES_2_NOV));
	}

	@Test
	void eliminarTurnoInexistente() {
		assertError("No existe el turno seleccionado", () -> model.eliminarTurnoEspecifico(999));
	}
}
