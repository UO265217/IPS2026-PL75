package giis.ips.empleados.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.empleados.GestionEmpleadosModel;
import giis.ips.empleados.HorarioController;
import giis.ips.empleados.HorarioEntity;
import giis.ips.empleados.HorarioModel;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 35203 (horarios periodicos de empleados no deportivos): recorren el flujo completo
 * controlador (validacion sintactica) -> modelo (reglas de negocio) -> base de datos.
 * Ana Garcia (id 1) es la unica empleada no deportiva y Luis Perez (id 2) es deportivo.
 * Los tests parten sin turnos para controlar exactamente las horas de cada dia.
 */
class TestHorario {
	private static final int ANA = 1;
	private static final int LUIS = 2;

	private Database db = new Database();
	private HorarioModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase(); // 4 empleados y los turnos de ejemplo de data.sql
		db.executeUpdate("delete from horario");
		model = new HorarioModel();
	}

	/** Ejecuta el flujo completo con los datos tal y como se escribirian en el formulario */
	private void anyadir(int empleadoId, String dia, String inicio, String fin) {
		model.anyadirTurno(HorarioController.crearTurno(empleadoId, dia, inicio, fin));
	}

	/** Comprueba el mensaje y que no se ha guardado ningun turno mas de los que habia */
	private void assertError(String mensaje, int turnosEsperados, Runnable accion) {
		ApplicationException ex = assertThrows(ApplicationException.class, accion::run);
		assertEquals(mensaje, ex.getMessage());
		assertEquals(turnosEsperados, db.executeQueryArray("select id from horario").size(),
				"Si hay error no se debe guardar nada");
	}

	// ---- Turnos validos ----

	@Test
	void anyadirUnTurno() {
		anyadir(ANA, "Lunes", "09:30", "13:15");
		List<HorarioEntity> turnos = model.getHorarios(ANA);
		assertEquals(1, turnos.size());
		assertEquals(1, turnos.get(0).getDiaSemana());
		assertEquals(9 * 60 + 30, turnos.get(0).getHoraInicio());
		assertEquals(13 * 60 + 15, turnos.get(0).getHoraFin());
		assertEquals(ANA, turnos.get(0).getEmpleadoId());
	}

	@Test
	void variosTurnosEnElMismoDiaOrdenadosPorHora() {
		anyadir(ANA, "Martes", "16:00", "19:00");
		anyadir(ANA, "Martes", "08:00", "12:00");
		List<HorarioEntity> turnos = model.getHorarios(ANA);
		assertEquals(2, turnos.size());
		assertEquals(8 * 60, turnos.get(0).getHoraInicio());
		assertEquals(16 * 60, turnos.get(1).getHoraInicio());
	}

	@Test
	void turnosPegadosNoSeSolapan() {
		anyadir(ANA, "Lunes", "09:00", "13:00");
		anyadir(ANA, "Lunes", "13:00", "17:00");
		assertEquals(2, model.getHorarios(ANA).size());
	}

	@Test
	void justoOchoHorasEnUnDia() {
		anyadir(ANA, "Miércoles", "00:00", "03:00");
		anyadir(ANA, "Miércoles", "10:00", "15:00");
		assertEquals(2, model.getHorarios(ANA).size());
	}

	@Test
	void justoCuarentaHorasEnLaSemana() {
		for (String dia : new String[] { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes" })
			anyadir(ANA, dia, "08:00", "16:00");
		assertEquals(5, model.getHorarios(ANA).size());
	}

	@Test
	void otroEmpleadoNoCuentaEnLosLimites() {
		// Dos empleados no deportivos distintos no comparten limites: se crea un segundo gerente de prueba
		db.executeUpdate("insert into empleado(nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion,"
				+ " salarioAnual) values ('Eva', 'Gil', '55555555K', '600555555', '1990-01-01', 'NO_DEPORTIVO',"
				+ " 'COCINA', 20000)");
		int eva = (Integer) db.executeQueryArray("select id from empleado where dni = '55555555K'").get(0)[0];
		anyadir(ANA, "Lunes", "08:00", "16:00");
		anyadir(eva, "Lunes", "08:00", "16:00");
		assertEquals(1, model.getHorarios(ANA).size());
		assertEquals(1, model.getHorarios(eva).size());
	}

	// ---- Errores de negocio ----

	@Test
	void empleadoDeportivoNoTieneHorario() {
		assertError("Solo se puede asignar horario a empleados no deportivos", 0,
				() -> anyadir(LUIS, "Lunes", "09:00", "13:00"));
	}

	@Test
	void empleadoInexistente() {
		assertError("No existe el empleado seleccionado", 0, () -> anyadir(999, "Lunes", "09:00", "13:00"));
	}

	@ParameterizedTest
	@CsvSource({ "13:00,09:00", "09:00,09:00" })
	void inicioNoAnteriorAlFin(String inicio, String fin) {
		assertError("La hora de inicio debe ser anterior a la hora de fin", 0,
				() -> anyadir(ANA, "Lunes", inicio, fin));
	}

	@Test
	void solapeParcialConOtroTurno() {
		anyadir(ANA, "Jueves", "09:00", "13:00");
		assertError("El turno se solapa con otro turno del mismo día", 1,
				() -> anyadir(ANA, "Jueves", "12:59", "15:00"));
	}

	@Test
	void solapeContenidoEnOtroTurno() {
		anyadir(ANA, "Jueves", "09:00", "17:00");
		assertError("El turno se solapa con otro turno del mismo día", 1,
				() -> anyadir(ANA, "Jueves", "10:00", "11:00"));
	}

	@Test
	void mismaHoraEnOtroDiaNoSolapa() {
		anyadir(ANA, "Jueves", "09:00", "13:00");
		anyadir(ANA, "Viernes", "09:00", "13:00");
		assertEquals(2, model.getHorarios(ANA).size());
	}

	@Test
	void ochoHorasYUnMinutoEnUnDia() {
		anyadir(ANA, "Miércoles", "00:00", "03:00");
		assertError("No se pueden superar las 8 horas de trabajo diarias", 1,
				() -> anyadir(ANA, "Miércoles", "10:00", "15:01"));
	}

	@Test
	void cuarentaHorasYUnMinutoEnLaSemana() {
		for (String dia : new String[] { "Lunes", "Martes", "Miércoles", "Jueves" })
			anyadir(ANA, dia, "08:00", "16:00");
		anyadir(ANA, "Viernes", "08:00", "15:59");
		assertError("No se pueden superar las 40 horas de trabajo semanales", 5,
				() -> anyadir(ANA, "Sábado", "08:00", "08:02"));
	}

	// ---- Eliminar turnos ----

	@Test
	void eliminarUnTurno() {
		anyadir(ANA, "Lunes", "09:00", "13:00");
		anyadir(ANA, "Lunes", "15:00", "18:00");
		model.eliminarTurno(model.getHorarios(ANA).get(0).getId());
		List<HorarioEntity> turnos = model.getHorarios(ANA);
		assertEquals(1, turnos.size());
		assertEquals(15 * 60, turnos.get(0).getHoraInicio());
	}

	@Test
	void eliminarTurnoInexistente() {
		ApplicationException ex = assertThrows(ApplicationException.class, () -> model.eliminarTurno(999));
		assertEquals("No existe el turno seleccionado", ex.getMessage());
	}

	private int idDe(String dni) {
		return (Integer) db.executeQueryArray("select id from empleado where dni = ?", dni).get(0)[0];
	}

	// ---- Lista de empleados y datos de ejemplo ----

	@Test
	void soloSeListanLosEmpleadosNoDeportivos() {
		assertEquals(5, model.getEmpleadosNoDeportivos().size());
		assertEquals(true, model.getEmpleadosNoDeportivos().stream().allMatch(e -> e.getTipo().equals("NO_DEPORTIVO")));
	}

	@Test
	void datosDeEjemploDeAna() {
		db.createDatabase(false);
		db.loadDatabase();
		assertEquals(6, model.getHorarios(ANA).size());
		assertEquals(10, model.getHorarios(idDe("66666666Z")).size());
		assertEquals(7, model.getHorarios(idDe("77777777B")).size());
		assertEquals(6, model.getHorarios(idDe("88888888Y")).size());
		assertEquals(0, model.getHorarios(idDe("99999999R")).size());
	}

	// ---- Relacion con la HU 35202: al eliminar un empleado se eliminan sus horarios ----

	@Test
	void alEliminarUnEmpleadoSeEliminanSusHorarios() {
		anyadir(ANA, "Lunes", "09:00", "13:00");
		new GestionEmpleadosModel().eliminarEmpleado(ANA);
		assertEquals(0, db.executeQueryArray("select id from horario").size());
	}

	// ---- Validaciones sintacticas del controlador ----

	@ParameterizedTest
	@CsvSource({ "9:00", "09-00", "ab:cd", "25:00", "24:01", "12:60", "12:5" })
	void formatoDeHoraNoValido(String hora) {
		ApplicationException ex = assertThrows(ApplicationException.class,
				() -> HorarioController.crearTurno(ANA, "Lunes", hora, "23:00"));
		assertEquals("La hora debe tener el formato HH:mm (entre 00:00 y 24:00)", ex.getMessage());
	}

	@Test
	void horaYDiaObligatorios() {
		assertEquals("Hay que indicar la hora de inicio", assertThrows(ApplicationException.class,
				() -> HorarioController.crearTurno(ANA, "Lunes", " ", "10:00")).getMessage());
		assertEquals("Hay que indicar la hora de fin", assertThrows(ApplicationException.class,
				() -> HorarioController.crearTurno(ANA, "Lunes", "09:00", "")).getMessage());
		assertEquals("Hay que seleccionar el día de la semana", assertThrows(ApplicationException.class,
				() -> HorarioController.crearTurno(ANA, null, "09:00", "10:00")).getMessage());
	}

	@Test
	void veinticuatroHorasEsValidoSoloComoFin() {
		anyadir(ANA, "Domingo", "20:00", "24:00");
		assertEquals(24 * 60, model.getHorarios(ANA).get(0).getHoraFin());
	}
}
