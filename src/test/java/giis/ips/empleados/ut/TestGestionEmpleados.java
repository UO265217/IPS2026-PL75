package giis.ips.empleados.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.empleados.EmpleadoEntity;
import giis.ips.empleados.GestionEmpleadosController;
import giis.ips.empleados.GestionEmpleadosModel;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 35202 (modificar y eliminar empleados): recorren el flujo completo
 * controlador (validacion sintactica) -> modelo (reglas de negocio) -> base de datos.
 * Se usa una fecha de hoy fija para que los limites de edad no dependan del dia de ejecucion.
 */
class TestGestionEmpleados {
	private static final LocalDate HOY = LocalDate.of(2026, 10, 14);

	private Database db = new Database();
	private GestionEmpleadosModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase(); // 8 empleados de data.sql
		model = new GestionEmpleadosModel();
	}

	/** Ejecuta el flujo completo con los datos tal y como se escribirian en el formulario */
	private void modificar(String dniOriginal, String nombre, String apellido, String dni, String telefono,
			String fecha, String salario) {
		EmpleadoEntity e = GestionEmpleadosController.crearEmpleadoModificado(buscar(dniOriginal), nombre, apellido,
				dni, telefono, fecha, salario);
		model.modificarEmpleado(e, HOY);
	}

	private EmpleadoEntity buscar(String dni) {
		List<EmpleadoEntity> lista = db.executeQueryPojo(EmpleadoEntity.class,
				"select * from empleado where dni = ?", dni);
		assertEquals(1, lista.size());
		return lista.get(0);
	}

	/** Comprueba el mensaje y que la BD no ha cambiado (Luis Perez sigue igual y siguen los 8 empleados) */
	private void assertError(String mensaje, Runnable accion) {
		ApplicationException ex = assertThrows(ApplicationException.class, accion::run);
		assertEquals(mensaje, ex.getMessage());
		assertEquals(8, model.getEmpleados().size(), "Si hay error no se debe borrar nada");
		EmpleadoEntity luis = buscar("22222222J");
		assertEquals("Luis", luis.getNombre());
		assertEquals(120000, luis.getSalarioAnual(), 0.001);
	}

	// ---- Modificaciones validas ----

	@Test
	void modificarDatosPersonalesYProfesionales() {
		modificar("22222222J", "Luis Miguel", "Pérez Gil", "55555555K", "611 222 333", "1995-04-15", "150000,75");
		EmpleadoEntity e = buscar("55555555K");
		assertEquals("Luis Miguel", e.getNombre());
		assertEquals("Pérez Gil", e.getApellido());
		assertEquals("611222333", e.getTelefono());
		assertEquals("1995-04-15", e.getFechaNacimiento());
		assertEquals(150000.75, e.getSalarioAnual(), 0.001);
		// el puesto de trabajo no cambia
		assertEquals("DEPORTIVO", e.getTipo());
		assertEquals("JUGADOR", e.getPosicion());
		assertEquals(8, model.getEmpleados().size());
	}

	@Test
	void mantenerElMismoDniNoEsDuplicado() {
		modificar("33333333P", "Marta", "López", "33333333P", "600333333", "1978-11-02", "85000");
		assertEquals(85000, buscar("33333333P").getSalarioAnual(), 0.001);
	}

	@Test
	void soloSeModificaElEmpleadoElegido() {
		modificar("33333333P", "Marta", "López", "33333333P", "600333333", "1978-11-02", "85000");
		assertEquals(60000, buscar("11111111H").getSalarioAnual(), 0.001);
		assertEquals(120000, buscar("22222222J").getSalarioAnual(), 0.001);
	}

	// ---- Mayoria de edad: limites (hoy = 2026-10-14) ----

	@Test
	void jugadorPuedeSerMenorDeEdad() {
		modificar("44444444A", "Hugo", "Fernández", "44444444A", "600444444", "2012-01-01", "15000.50");
		assertEquals("2012-01-01", buscar("44444444A").getFechaNacimiento());
	}

	@Test
	void entrenadorQueCumple18HoyEsValido() {
		modificar("33333333P", "Marta", "López", "33333333P", "600333333", "2008-10-14", "80000");
		assertEquals("2008-10-14", buscar("33333333P").getFechaNacimiento());
	}

	@Test
	void entrenadorAlQueLeFaltaUnDiaPara18NoEsValido() {
		assertError("Solo los jugadores pueden ser menores de edad", () -> modificar("33333333P", "Marta", "López",
				"33333333P", "600333333", "2008-10-15", "80000"));
		assertEquals("1978-11-02", buscar("33333333P").getFechaNacimiento());
	}

	@Test
	void fechaNacimientoFuturaNoEsValida() {
		assertError("La fecha de nacimiento no puede ser posterior a hoy", () -> modificar("22222222J", "Luis",
				"Pérez", "22222222J", "600222222", "2026-10-15", "120000"));
	}

	// ---- Reglas de negocio del modelo ----

	@Test
	void dniDeOtroEmpleadoNoEsValido() {
		assertError("Ya existe otro empleado con el DNI 11111111H", () -> modificar("22222222J", "Luis", "Pérez",
				"11111111H", "600222222", "1995-03-14", "120000"));
	}

	@ParameterizedTest
	@CsvSource({ "0", "-1000" })
	void salarioNoPositivoNoEsValido(String salario) {
		assertError("El salario bruto anual debe ser mayor que 0", () -> modificar("22222222J", "Luis", "Pérez",
				"22222222J", "600222222", "1995-03-14", salario));
	}

	@Test
	void noSePuedeCambiarElPuestoDeTrabajo() {
		// la vista no lo permite (tipo y posicion no son editables), pero el modelo no debe confiar en ella
		EmpleadoEntity e = buscar("22222222J");
		e.setPosicion("ENTRENADOR");
		assertError("No se puede cambiar el puesto de trabajo de un empleado", () -> model.modificarEmpleado(e, HOY));
		e.setTipo("NO_DEPORTIVO");
		e.setPosicion("GERENTE");
		assertError("No se puede cambiar el puesto de trabajo de un empleado", () -> model.modificarEmpleado(e, HOY));
	}

	@Test
	void modificarEmpleadoInexistenteNoEsValido() {
		EmpleadoEntity e = buscar("22222222J");
		e.setId(999);
		assertError("No existe el empleado seleccionado", () -> model.modificarEmpleado(e, HOY));
	}

	// ---- Validaciones sintacticas (las mismas que en el alta) ----

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"''   | Pérez | 22222222J | 600222222 | 1995-03-14 | 120000 | El campo nombre es obligatorio",
			"Luis | Pérez | 22222222A | 600222222 | 1995-03-14 | 120000 | La letra del DNI no es correcta",
			"Luis | Pérez | 22222222J | 60022222  | 1995-03-14 | 120000 | El teléfono debe tener 9 cifras",
			"Luis | Pérez | 22222222J | 600222222 | 14/03/1995 | 120000 | La fecha de nacimiento debe ser una fecha válida con formato aaaa-mm-dd",
			"Luis | Pérez | 22222222J | 600222222 | 1995-03-14 | abc    | El salario bruto anual debe ser un número" })
	void datosConFormatoIncorrecto(String nombre, String apellido, String dni, String telefono, String fecha,
			String salario, String mensaje) {
		assertError(mensaje, () -> modificar("22222222J", nombre, apellido, dni, telefono, fecha, salario));
	}

	// ---- Eliminacion ----

	@Test
	void eliminarEmpleado() {
		int id = buscar("22222222J").getId();
		model.eliminarEmpleado(id);
		List<EmpleadoEntity> empleados = model.getEmpleados();
		assertEquals(7, empleados.size());
		assertTrue(empleados.stream().noneMatch(e -> e.getDni().equals("22222222J")));
		assertTrue(db.executeQueryArray("select id from empleado where id = ?", id).isEmpty());
	}

	@Test
	void eliminarEmpleadoInexistenteNoEsValido() {
		assertError("No existe el empleado seleccionado", () -> model.eliminarEmpleado(999));
	}

	@Test
	void trasEliminarSuDniSePuedeVolverAUsar() {
		model.eliminarEmpleado(buscar("22222222J").getId());
		modificar("44444444A", "Hugo", "Fernández", "22222222J", "600444444", "2010-07-30", "15000.50");
		assertEquals("Hugo", buscar("22222222J").getNombre());
	}
}
