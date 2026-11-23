package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import giis.ips.login.LoginModel;
import giis.ips.login.Rol;
import giis.ips.login.Sesion;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de la HU 37116 (login con roles o invitado): reglas de negocio del modelo contra la base de datos
 * con las cuentas de ejemplo de data.sql (contrasena = nombre de usuario + 123)
 */
class TestLoginModel {
	private static final String ERROR_CREDENCIALES = "Usuario o contraseña incorrectos";

	private Database db = new Database();
	private LoginModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new LoginModel();
	}

	// ---- Inicio de sesion con cuenta ----

	@ParameterizedTest
	@CsvSource({ "ana,GERENTE", "marta,ENTRENADOR", "carlos,VENDEDOR_ENTRADAS", "lucia,ENCARGADO_TIENDA",
			"pedro,JARDINERIA", "marcos,JARDINERIA" })
	void cuentaValidaDaSuRol(String usuario, Rol rol) {
		Sesion s = model.iniciarSesion(usuario, usuario + "123");
		assertEquals(rol, s.getRol());
		assertEquals(usuario, s.getNombreUsuario());
		assertFalse(s.esInvitado());
	}

	@Test
	void laSesionGuardaElEmpleadoDeLaCuenta() {
		Sesion s = model.iniciarSesion("ana", "ana123");
		Object[] fila = db.executeQueryArray("select id from empleado where dni = '11111111H'").get(0);
		assertEquals(((Number) fila[0]).intValue(), s.getEmpleadoId());
	}

	@Test
	void espaciosAlrededorDelUsuarioSeIgnoran() {
		assertEquals(Rol.GERENTE, model.iniciarSesion("  ana  ", "ana123").getRol());
	}

	// ---- Errores ----

	@Test
	void contrasenaIncorrecta() {
		assertError(ERROR_CREDENCIALES, () -> model.iniciarSesion("ana", "otra"));
	}

	@Test
	void usuarioInexistenteDaElMismoErrorQueContrasenaIncorrecta() {
		assertError(ERROR_CREDENCIALES, () -> model.iniciarSesion("fantasma", "ana123"));
	}

	@Test
	void laContrasenaDistingueMayusculas() {
		assertError(ERROR_CREDENCIALES, () -> model.iniciarSesion("ana", "ANA123"));
	}

	@Test
	void elUsuarioDistingueMayusculas() {
		assertError(ERROR_CREDENCIALES, () -> model.iniciarSesion("Ana", "ana123"));
	}

	@Test
	void noSeAceptaElHashComoContrasena() {
		String hash = giis.ips.login.Contrasena.hash("ana123");
		assertError(ERROR_CREDENCIALES, () -> model.iniciarSesion("ana", hash));
	}

	@ParameterizedTest
	@CsvSource({ "'',ana123", "ana,''", "'   ',ana123", "ana,'   '" })
	void usuarioOContrasenaVacios(String usuario, String contrasena) {
		assertError("Introduce el usuario y la contraseña", () -> model.iniciarSesion(usuario, contrasena));
	}

	@Test
	void datosNulos() {
		assertError("Introduce el usuario y la contraseña", () -> model.iniciarSesion(null, "x"));
		assertError("Introduce el usuario y la contraseña", () -> model.iniciarSesion("ana", null));
	}

	@Test
	void cuentaDeEmpleadoConPosicionSinRol() {
		db.executeUpdate("update empleado set posicion = 'DESCONOCIDA' where dni = '11111111H'");
		assertError("La cuenta no tiene un rol válido", () -> model.iniciarSesion("ana", "ana123"));
	}

	// ---- Invitado ----

	@Test
	void invitadoEntraSinCuenta() {
		Sesion s = model.entrarComoInvitado();
		assertEquals(Rol.INVITADO, s.getRol());
		assertTrue(s.esInvitado());
		assertNull(s.getNombreUsuario());
		assertEquals(0, s.getEmpleadoId());
	}

	private void assertError(String mensaje, Runnable accion) {
		ApplicationException ex = assertThrows(ApplicationException.class, accion::run);
		assertEquals(mensaje, ex.getMessage());
	}
}
