package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import giis.ips.login.Contrasena;
import giis.ips.login.CuentaModel;
import giis.ips.login.LoginModel;
import giis.ips.login.Rol;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Pruebas de las cuentas de los empleados (HU 37116): crear, cambiar y borrar contra la base de datos de ejemplo.
 * Ana (gerente) tiene la cuenta "ana"; Sara (directora de comunicaciones) no tiene cuenta
 */
class TestCuentaModel {
	private Database db = new Database();
	private CuentaModel model;
	private int anaId;
	private int saraId;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new CuentaModel();
		anaId = empleadoId("ana");
		saraId = ((Number) db.executeQueryArray("select id from empleado where posicion = 'DIRECTOR_COMUNICACIONES'")
				.get(0)[0]).intValue();
	}

	private int empleadoId(String usuario) {
		return ((Number) db.executeQueryArray("select empleadoId from usuario where nombreUsuario = ?", usuario)
				.get(0)[0]).intValue();
	}

	private void assertError(String mensaje, Runnable accion) {
		assertEquals(mensaje, assertThrows(ApplicationException.class, accion::run).getMessage());
	}

	@Test
	void empleadoSinCuentaNoTieneUsuario() {
		assertNull(model.getNombreUsuario(saraId));
		assertEquals("ana", model.getNombreUsuario(anaId));
	}

	@Test
	void cuentaNuevaPermiteIniciarSesion() {
		model.crear(saraId, "sara", "clave1");
		assertEquals(Rol.DIRECTOR_COMUNICACIONES, new LoginModel().iniciarSesion("sara", "clave1").getRol());
	}

	@Test
	void laContrasenaSeGuardaConHashYNoEnClaro() {
		model.crear(saraId, "sara", "clave1");
		List<Object[]> filas = db.executeQueryArray("select contrasena from usuario where nombreUsuario = 'sara'");
		assertEquals(Contrasena.hash("clave1"), filas.get(0)[0]);
	}

	@Test
	void usuarioRepetidoNoEsValido() {
		assertError("Ya existe una cuenta con el usuario ana", () -> model.crear(saraId, "ana", "clave1"));
		assertNull(model.getNombreUsuario(saraId));
	}

	@Test
	void empleadoConCuentaNoPuedeTenerOtra() {
		assertError("El empleado ya tiene una cuenta", () -> model.crear(anaId, "ana2", "clave1"));
	}

	@Test
	void usuarioOContrasenaVaciosNoSonValidos() {
		assertError("El usuario es obligatorio", () -> model.crear(saraId, " ", "clave1"));
		assertError("La contraseña es obligatoria", () -> model.crear(saraId, "sara", ""));
	}

	@Test
	void usuarioDemasiadoLargoNoEsValido() {
		assertError("El usuario admite como mucho 30 caracteres", () -> model.crear(saraId, "x".repeat(31), "clave1"));
	}

	@Test
	void gerenteCambiaUsuarioYContrasena() {
		model.modificar(anaId, "ana.gerente", "nueva1");
		LoginModel login = new LoginModel();
		assertEquals(Rol.GERENTE, login.iniciarSesion("ana.gerente", "nueva1").getRol());
		assertThrows(ApplicationException.class, () -> login.iniciarSesion("ana", "ana123"));
	}

	@Test
	void contrasenaVaciaConservaLaAnterior() {
		model.modificar(anaId, "ana.gerente", "");
		assertEquals(Rol.GERENTE, new LoginModel().iniciarSesion("ana.gerente", "ana123").getRol());
	}

	@Test
	void mantenerElMismoUsuarioNoEsDuplicado() {
		model.modificar(anaId, "ana", "nueva1");
		assertEquals(Rol.GERENTE, new LoginModel().iniciarSesion("ana", "nueva1").getRol());
	}

	@Test
	void cambiarAlUsuarioDeOtroNoEsValido() {
		assertError("Ya existe una cuenta con el usuario marta", () -> model.modificar(anaId, "marta", ""));
		assertEquals("ana", model.getNombreUsuario(anaId));
	}

	@Test
	void modificarCreaLaCuentaSiNoLaTenia() {
		model.modificar(saraId, "sara", "clave1");
		assertEquals("sara", model.getNombreUsuario(saraId));
	}

	@Test
	void cuentaNuevaSinContrasenaNoEsValida() {
		assertError("La contraseña es obligatoria para crear la cuenta", () -> model.modificar(saraId, "sara", ""));
		assertNull(model.getNombreUsuario(saraId));
	}

	@Test
	void sinUsuarioNiContrasenaSeQuedaSinCuenta() {
		model.modificar(saraId, "", "");
		assertNull(model.getNombreUsuario(saraId));
	}

	@Test
	void unaCuentaExistenteNoPuedeQuedarseSinUsuario() {
		assertError("El usuario es obligatorio", () -> model.modificar(anaId, "", ""));
		assertEquals("ana", model.getNombreUsuario(anaId));
	}

	@Test
	void eliminarBorraLaCuenta() {
		model.eliminar(anaId);
		assertNull(model.getNombreUsuario(anaId));
	}
}
