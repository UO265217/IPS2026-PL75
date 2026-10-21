package giis.ips.empleados.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import giis.ips.empleados.HorarioEntity;
import giis.ips.empleados.HorarioModel;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 35205 (horario de un empleado no deportivo en una semana concreta): parten de los datos
 * de ejemplo de data.sql, con horarios de dia especifico en las semanas del 19/10/2026 y del 26/10/2026.
 * Cada turno se resume como "dia inicio-fin" (minutos) y con un asterisco si es de dia especifico.
 */
class TestHorarioSemana {
	private static final LocalDate MARTES_20_OCT = LocalDate.of(2026, 10, 20);

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

	private void anyadirEspecifico(String dni, String fecha, int inicio, int fin) {
		db.executeUpdate("insert into horario_especifico(empleadoId, fecha, horaInicio, horaFin) values (?, ?, ?, ?)",
				idDe(dni), fecha, inicio, fin);
	}

	/** Turnos de la semana resumidos en texto para compararlos de una vez */
	private String semana(String dni, LocalDate fecha) {
		StringBuilder sb = new StringBuilder();
		for (HorarioEntity h : model.getHorarioSemana(idDe(dni), fecha))
			sb.append(h.getDiaSemana()).append(" ").append(h.getHoraInicio()).append("-").append(h.getHoraFin())
					.append(h.isEspecifico() ? "*" : "").append("\n");
		return sb.toString();
	}

	private int totalMinutos(String dni, LocalDate fecha) {
		int total = 0;
		for (HorarioEntity h : model.getHorarioSemana(idDe(dni), fecha))
			total += h.getHoraFin() - h.getHoraInicio();
		return total;
	}

	// ---- Semana con solo horario periodico ----

	@Test
	void semanaSinDiasEspecificosEsElHorarioPeriodico() {
		assertEquals("1 540-840\n1 960-1080\n2 540-840\n3 540-840\n4 540-840\n5 540-840\n",
				semana("11111111H", LocalDate.of(2026, 10, 12)));
	}

	@Test
	void empleadoSinHorarioTieneLaSemanaVacia() {
		assertEquals("", semana("99999999R", MARTES_20_OCT));
	}

	// ---- Dias especificos ----

	@Test
	void diaEspecificoSustituyeAlPeriodicoDeEseDia() {
		assertEquals("1 540-840\n1 960-1080\n2 600-1080*\n3 540-840\n4 540-840\n5 540-840\n",
				semana("11111111H", MARTES_20_OCT));
		assertEquals(30 * 60, totalMinutos("11111111H", MARTES_20_OCT));
	}

	@Test
	void diaEspecificoSustituyeTodosLosTurnosDelDia() {
		// Carlos tiene turno partido; el miercoles 21 solo queda el turno especifico
		String semana = semana("66666666Z", MARTES_20_OCT);
		assertEquals(true, semana.contains("3 480-960*\n"));
		assertEquals(false, semana.contains("3 540-780\n"));
		assertEquals(false, semana.contains("3 960-1200\n"));
		assertEquals(40 * 60, totalMinutos("66666666Z", MARTES_20_OCT));
	}

	@Test
	void diaEspecificoEnUnDiaSinHorarioPeriodico() {
		assertEquals("1 420-780\n2 420-780\n3 420-780\n4 420-780\n5 420-780\n6 420-780\n7 480-720*\n",
				semana("88888888Y", MARTES_20_OCT));
	}

	@Test
	void diaEspecificoConVariosTurnosOrdenados() {
		assertEquals("4 540-720*\n4 780-900*\n5 600-840\n5 1020-1260\n6 600-840\n6 1020-1260\n7 600-840\n",
				semana("77777777B", LocalDate.of(2026, 10, 26)));
	}

	@Test
	void diaEspecificoDeOtraSemanaNoAparece() {
		// El dia especifico de Lucia es el 29/10: en la semana anterior tiene su horario periodico
		assertEquals("4 600-840\n4 1020-1260\n5 600-840\n5 1020-1260\n6 600-840\n6 1020-1260\n7 600-840\n",
				semana("77777777B", MARTES_20_OCT));
	}

	@Test
	void diaEspecificoDeOtroEmpleadoNoAparece() {
		// El martes 20 es especifico solo para Ana; Carlos mantiene su turno partido
		assertEquals(true, semana("66666666Z", MARTES_20_OCT).contains("2 540-780\n2 960-1200\n"));
	}

	// ---- Calculo de la semana a partir de la fecha ----

	@ParameterizedTest
	@ValueSource(strings = { "2026-10-19", "2026-10-22", "2026-10-25" })
	void cualquierDiaDeLaSemanaDaLaMismaSemana(String fecha) {
		assertEquals(semana("11111111H", MARTES_20_OCT), semana("11111111H", LocalDate.parse(fecha)));
	}

	@Test
	void elLunesSiguienteEsOtraSemana() {
		assertEquals(false, semana("11111111H", LocalDate.of(2026, 10, 26)).contains("*"));
	}

	@Test
	void lunesDeLaSemana() {
		assertEquals(LocalDate.of(2026, 10, 19), HorarioModel.getLunes(LocalDate.of(2026, 10, 19)));
		assertEquals(LocalDate.of(2026, 10, 19), HorarioModel.getLunes(LocalDate.of(2026, 10, 25)));
		assertEquals(LocalDate.of(2026, 12, 28), HorarioModel.getLunes(LocalDate.of(2027, 1, 1)));
	}

	@Test
	void semanaQueCambiaDeMes() {
		// Semana del 26/10 al 01/11: el domingo 1 de noviembre pertenece a ella
		anyadirEspecifico("11111111H", "2026-11-01", 600, 720);
		assertEquals(true, semana("11111111H", LocalDate.of(2026, 10, 28)).contains("7 600-720*\n"));
	}

	@Test
	void semanaQueCambiaDeAnyo() {
		// Semana del 28/12/2026 al 03/01/2027: se pide por el jueves 31 y el especifico es el viernes 1
		anyadirEspecifico("11111111H", "2027-01-01", 600, 720);
		String semana = semana("11111111H", LocalDate.of(2026, 12, 31));
		assertEquals(true, semana.contains("5 600-720*\n"));
		assertEquals(false, semana.contains("5 540-840\n"));
	}

	// ---- Errores ----

	@Test
	void empleadoDeportivoNoTieneHorario() {
		ApplicationException ex = assertThrows(ApplicationException.class,
				() -> model.getHorarioSemana(idDe("22222222J"), MARTES_20_OCT));
		assertEquals("Solo los empleados no deportivos tienen horario", ex.getMessage());
	}

	@Test
	void empleadoInexistente() {
		ApplicationException ex = assertThrows(ApplicationException.class,
				() -> model.getHorarioSemana(999, MARTES_20_OCT));
		assertEquals("No existe el empleado seleccionado", ex.getMessage());
	}
}
