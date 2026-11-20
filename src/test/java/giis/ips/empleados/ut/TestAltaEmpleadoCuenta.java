package giis.ips.empleados.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import giis.ips.empleados.AltaEmpleadoController;
import giis.ips.empleados.AltaEmpleadoModel;
import giis.ips.empleados.EmpleadoEntity;
import giis.ips.login.LoginModel;
import giis.ips.login.Rol;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la cuenta que se define al dar de alta al empleado (HU 37116 y HU 35201): usuario y contrasena
 * obligatorios, y el empleado nuevo entra con el rol de su posicion
 */
class TestAltaEmpleadoCuenta {
	private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

	private Database db = new Database();
	private AltaEmpleadoModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase(); // 9 empleados y 6 cuentas de data.sql
		model = new AltaEmpleadoModel();
	}

	private void alta(String dni, String posicion, String usuario, String contrasena) {
		EmpleadoEntity e = AltaEmpleadoController.crearEmpleado("Nuevo", "Empleado", dni, "600000000", "1990-01-01",
				"No deportivo", posicion, "30000");
		model.altaEmpleado(e, HOY, AltaEmpleadoController.validarUsuario(usuario),
				AltaEmpleadoController.validarContrasena(contrasena));
	}

	private void assertError(String mensaje, Runnable accion) {
		assertEquals(mensaje, assertThrows(ApplicationException.class, accion::run).getMessage());
		assertEquals(9, model.getEmpleados().size(), "Si hay error no se debe guardar ningun empleado");
		assertEquals(6, db.executeQueryArray("select id from usuario").size(), "ni ninguna cuenta");
	}

	@Test
	void empleadoNuevoEntraConElRolDeSuPosicion() {
		alta("12345678Z", "Gestor de instalaciones", "nuevo", "clave1");
		assertEquals(Rol.GESTOR_INSTALACIONES, new LoginModel().iniciarSesion("nuevo", "clave1").getRol());
	}

	@Test
	void usuarioRepetidoNoGuardaNingunEmpleado() {
		assertError("Ya existe una cuenta con el usuario ana", () -> alta("12345678Z", "Cocina", "ana", "clave1"));
	}

	@Test
	void sinUsuarioONiContrasenaNoEsValido() {
		assertError("El campo usuario es obligatorio", () -> alta("12345678Z", "Cocina", "  ", "clave1"));
		assertError("El campo contraseña es obligatorio", () -> alta("12345678Z", "Cocina", "nuevo", ""));
	}

	@Test
	void usuarioConEspaciosONoValido() {
		assertError("El usuario no puede tener espacios", () -> alta("12345678Z", "Cocina", "dos palabras", "clave1"));
		assertError("El usuario admite como mucho 30 caracteres",
				() -> alta("12345678Z", "Cocina", "x".repeat(31), "clave1"));
	}

	@Test
	void siFallaElEmpleadoNoSeGuardaLaCuenta() {
		// el DNI 11111111H ya existe (Ana): el alta falla y no queda la cuenta "nuevo"
		assertError("Ya existe un empleado con el DNI 11111111H", () -> alta("11111111H", "Cocina", "nuevo", "clave1"));
	}
}
