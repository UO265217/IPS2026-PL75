package giis.ips.empleados.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.empleados.AltaEmpleadoController;
import giis.ips.empleados.AltaEmpleadoModel;
import giis.ips.empleados.EmpleadoEntity;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 35201 (alta de empleados): recorren el flujo completo
 * controlador (validacion sintactica) -> modelo (reglas de negocio) -> base de datos.
 * Se usa una fecha de hoy fija para que los limites de edad no dependan del dia de ejecucion.
 */
class TestAltaEmpleado {
	private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

	private Database db = new Database();
	private AltaEmpleadoModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase(); // 4 empleados de data.sql
		model = new AltaEmpleadoModel();
	}

	/** Ejecuta el flujo completo con los datos tal y como se escribirian en el formulario */
	private void alta(String nombre, String apellido, String dni, String telefono, String fecha, String tipo,
			String posicion, String salario) {
		EmpleadoEntity e = AltaEmpleadoController.crearEmpleado(nombre, apellido, dni, telefono, fecha, tipo, posicion,
				salario);
		model.altaEmpleado(e, HOY);
	}

	private EmpleadoEntity buscar(String dni) {
		List<EmpleadoEntity> lista = db.executeQueryPojo(EmpleadoEntity.class,
				"select * from empleado where dni = ?", dni);
		assertEquals(1, lista.size());
		return lista.get(0);
	}

	private void assertError(String mensaje, Runnable accion) {
		ApplicationException ex = assertThrows(ApplicationException.class, accion::run);
		assertEquals(mensaje, ex.getMessage());
		assertEquals(4, model.getEmpleados().size(), "Si hay error no se debe guardar nada");
	}

	// ---- Altas validas ----

	@Test
	void altaEmpleadoNoDeportivo() {
		alta("Eva", "Ruiz", "55555555K", "600 555 555", "1990-01-15", "No deportivo", "Vendedor de entradas/abonos",
				"24000,50");
		EmpleadoEntity e = buscar("55555555K");
		assertEquals("Eva", e.getNombre());
		assertEquals("Ruiz", e.getApellido());
		assertEquals("600555555", e.getTelefono());
		assertEquals("1990-01-15", e.getFechaNacimiento());
		assertEquals("NO_DEPORTIVO", e.getTipo());
		assertEquals("VENDEDOR_ENTRADAS", e.getPosicion());
		assertEquals(24000.50, e.getSalarioAnual(), 0.001);
		assertEquals(5, model.getEmpleados().size());
	}

	@Test
	void altaJugadorMenorDeEdad() {
		alta("Leo", "Martín", "12345678Z", "600123456", "2012-06-01", "Deportivo", "Jugador", "9000");
		assertEquals("JUGADOR", buscar("12345678Z").getPosicion());
	}

	@Test
	void altaEntrenadorAdulto() {
		alta("Raúl", "Sanz", "87654321X", "600654321", "1985-09-09", "Deportivo", "Entrenador", "70000");
		assertEquals("DEPORTIVO", buscar("87654321X").getTipo());
	}

	@Test
	void altaDniEnMinusculaSeGuardaEnMayuscula() {
		alta("Eva", "Ruiz", "55555555k", "600555555", "1990-01-15", "No deportivo", "Cocina", "20000");
		buscar("55555555K");
	}

	// ---- Mayoria de edad: limites (hoy = 2026-10-12) ----

	@Test
	void noDeportivoQueCumple18HoyEsValido() {
		alta("Eva", "Ruiz", "55555555K", "600555555", "2008-10-12", "No deportivo", "Jardinería", "18000");
		buscar("55555555K");
	}

	@Test
	void noDeportivoAlQueLeFaltaUnDiaPara18NoEsValido() {
		assertError("Solo se pueden dar de alta menores de edad como jugadores", () -> alta("Eva", "Ruiz",
				"55555555K", "600555555", "2008-10-13", "No deportivo", "Jardinería", "18000"));
	}

	@Test
	void entrenadorMenorNoEsValido() {
		assertError("Solo se pueden dar de alta menores de edad como jugadores", () -> alta("Eva", "Ruiz",
				"55555555K", "600555555", "2010-01-01", "Deportivo", "Entrenador", "18000"));
	}

	@Test
	void fechaNacimientoFuturaNoEsValida() {
		assertError("La fecha de nacimiento no puede ser posterior a hoy", () -> alta("Eva", "Ruiz", "55555555K",
				"600555555", "2026-10-13", "Deportivo", "Jugador", "18000"));
	}

	// ---- Reglas de negocio del modelo ----

	@Test
	void dniRepetidoNoEsValido() {
		assertError("Ya existe un empleado con el DNI 22222222J", () -> alta("Otro", "Jugador", "22222222J",
				"600555555", "1990-01-01", "Deportivo", "Jugador", "18000"));
	}

	@ParameterizedTest
	@CsvSource({ "0", "-1000" })
	void salarioNoPositivoNoEsValido(String salario) {
		assertError("El salario bruto anual debe ser mayor que 0", () -> alta("Eva", "Ruiz", "55555555K",
				"600555555", "1990-01-15", "No deportivo", "Cocina", salario));
	}

	@Test
	void posicionQueNoCorrespondeAlTipoNoEsValida() {
		// el formulario no lo permite (el combo filtra), pero el modelo no debe confiar en la vista
		assertError("La posición Cocina no corresponde a un empleado deportivo", () -> alta("Eva", "Ruiz",
				"55555555K", "600555555", "1990-01-15", "Deportivo", "Cocina", "18000"));
	}

	// ---- Validaciones sintacticas del controlador ----

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"''        | Ruiz | 55555555K | 600555555  | 1990-01-15 | 20000    | El campo nombre es obligatorio",
			"Eva       | ''   | 55555555K | 600555555  | 1990-01-15 | 20000    | El campo apellido es obligatorio",
			"Eva       | Ruiz | ''        | 600555555  | 1990-01-15 | 20000    | El campo DNI es obligatorio",
			"Eva       | Ruiz | 55555555K | ''         | 1990-01-15 | 20000    | El campo teléfono es obligatorio",
			"Eva       | Ruiz | 55555555K | 600555555  | ''         | 20000    | El campo fecha de nacimiento es obligatorio",
			"Eva       | Ruiz | 55555555K | 600555555  | 1990-01-15 | ''       | El campo salario bruto anual es obligatorio",
			"Eva       | Ruiz | 5555555K  | 600555555  | 1990-01-15 | 20000    | El DNI debe tener 8 cifras y una letra",
			"Eva       | Ruiz | 55555555K | 60055555   | 1990-01-15 | 20000    | El teléfono debe tener 9 cifras",
			"Eva       | Ruiz | 55555555K | 6005a5555  | 1990-01-15 | 20000    | El teléfono debe tener 9 cifras",
			"Eva       | Ruiz | 55555555K | 600555555  | 15/01/1990 | 20000    | La fecha de nacimiento debe ser una fecha válida con formato aaaa-mm-dd",
			"Eva       | Ruiz | 55555555K | 600555555  | 2026-02-30 | 20000    | La fecha de nacimiento debe ser una fecha válida con formato aaaa-mm-dd",
			"Eva       | Ruiz | 55555555K | 600555555  | 1990-01-15 | abc      | El salario bruto anual debe ser un número",
			"Eva       | Ruiz | 55555555K | 600555555  | 1990-01-15 | 20000.555 | El salario bruto anual admite como mucho 2 decimales" })
	void datosConFormatoIncorrecto(String nombre, String apellido, String dni, String telefono, String fecha,
			String salario, String mensaje) {
		assertError(mensaje,
				() -> alta(nombre, apellido, dni, telefono, fecha, "No deportivo", "Cocina", salario));
	}

	@Test
	void sinTipoOPosicionNoEsValido() {
		assertError("Hay que seleccionar el tipo de empleado y la posición", () -> alta("Eva", "Ruiz",
				"55555555K", "600555555", "1990-01-15", "No deportivo", null, "20000"));
	}
}
