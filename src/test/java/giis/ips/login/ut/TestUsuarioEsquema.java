package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import giis.ips.util.Database;

/**
 * Pruebas del esquema de la HU 37116 (login con roles o invitado): tabla usuario y sus datos de ejemplo.
 * El rol de un usuario es la posicion del empleado al que pertenece la cuenta.
 */
class TestUsuarioEsquema {
	private Database db = new Database();

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
	}

	@Test
	void usuariosDeEjemploConSuEmpleado() {
		List<Object[]> filas = db.executeQueryArray(
				"select u.nombreUsuario, e.posicion from usuario u join empleado e on e.id = u.empleadoId"
						+ " where u.nombreUsuario = ?", "ana");
		assertEquals(1, filas.size());
		assertEquals("GERENTE", filas.get(0)[1]);
	}

	@Test
	void unUsuarioPorEmpleadoComoMucho() {
		List<Object[]> filas = db.executeQueryArray("select empleadoId, count(*) from usuario group by empleadoId");
		for (Object[] f : filas)
			assertEquals(1, ((Number) f[1]).intValue());
	}

	@Test
	void nombreDeUsuarioUnico() {
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"insert into usuario(nombreUsuario, contrasena, empleadoId) select 'ana', 'x', id from empleado where dni = ?",
				"22222222J"));
	}

	@Test
	void unEmpleadoNoPuedeTenerDosCuentas() {
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"insert into usuario(nombreUsuario, contrasena, empleadoId) select 'ana2', 'x', id from empleado where dni = ?",
				"11111111H"));
	}

	@Test
	void nombreYContrasenaObligatorios() {
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"insert into usuario(nombreUsuario, contrasena, empleadoId) select '', 'x', id from empleado where dni = ?",
				"22222222J"));
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"insert into usuario(nombreUsuario, contrasena, empleadoId) select 'luis', '', id from empleado where dni = ?",
				"22222222J"));
	}
}
