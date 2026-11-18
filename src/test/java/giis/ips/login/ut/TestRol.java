package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import giis.ips.empleados.Posicion;
import giis.ips.login.Rol;

/**
 * Pruebas de los roles de la HU 37116: cada posicion de empleado es un rol y existe el rol invitado
 */
class TestRol {

	@Test
	void cadaPosicionTieneSuRol() {
		for (Posicion p : Posicion.values())
			assertEquals(p, Rol.deEmpleado(p.name()).getPosicion());
	}

	@Test
	void invitadoNoEsDeNingunEmpleado() {
		assertTrue(Rol.INVITADO.esInvitado());
		assertEquals(null, Rol.INVITADO.getPosicion());
		assertFalse(Rol.GERENTE.esInvitado());
	}

	@Test
	void posicionDesconocidaNoTieneRol() {
		assertEquals(null, Rol.deEmpleado("INEXISTENTE"));
		assertEquals(null, Rol.deEmpleado(null));
	}
}
