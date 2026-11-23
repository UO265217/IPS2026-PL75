package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import giis.ips.login.Funcionalidad;
import giis.ips.login.Permisos;
import giis.ips.login.Rol;
import giis.ips.util.ApplicationException;

/**
 * Pruebas de los permisos por rol de la HU 37116
 */
class TestPermisos {

	@Test
	void gerenteAccedeATodo() {
		assertEquals(EnumSet.allOf(Funcionalidad.class), Permisos.deRol(Rol.GERENTE));
	}

	@Test
	void invitadoSoloPuedeReservar() {
		assertEquals(EnumSet.of(Funcionalidad.RESERVAR_INSTALACION), Permisos.deRol(Rol.INVITADO));
		assertFalse(Permisos.puedeAcceder(Rol.INVITADO, Funcionalidad.ALTA_EMPLEADOS));
	}

	@Test
	void soloElGerenteGestionaEmpleados() {
		for (Rol r : Rol.values()) {
			boolean esGerente = r == Rol.GERENTE;
			assertEquals(esGerente, Permisos.puedeAcceder(r, Funcionalidad.ALTA_EMPLEADOS), r.name());
			assertEquals(esGerente, Permisos.puedeAcceder(r, Funcionalidad.GESTION_EMPLEADOS), r.name());
			assertEquals(esGerente, Permisos.puedeAcceder(r, Funcionalidad.HORARIOS_EMPLEADOS), r.name());
		}
	}

	@Test
	void todoEmpleadoVeSuHorario() {
		for (Rol r : Rol.values())
			assertEquals(!r.esInvitado(), Permisos.puedeAcceder(r, Funcionalidad.MI_HORARIO_SEMANAL), r.name());
	}

	@Test
	void funcionalidadPropiaDelPuesto() {
		assertTrue(Permisos.puedeAcceder(Rol.ENTRENADOR, Funcionalidad.ASIGNAR_ENTRENAMIENTO));
		assertFalse(Permisos.puedeAcceder(Rol.ENTRENADOR, Funcionalidad.PROGRAMAR_JARDINERIA));
		assertTrue(Permisos.puedeAcceder(Rol.JARDINERIA, Funcionalidad.PROGRAMAR_JARDINERIA));
		assertFalse(Permisos.puedeAcceder(Rol.COCINA, Funcionalidad.PROGRAMAR_JARDINERIA));
	}

	@Test
	void gestorDeInstalacionesSoloTieneLasReservas() {
		assertTrue(Permisos.puedeAcceder(Rol.GESTOR_INSTALACIONES, Funcionalidad.RESERVAR_INSTALACION));
		assertFalse(Permisos.puedeAcceder(Rol.GESTOR_INSTALACIONES, Funcionalidad.ASIGNAR_ENTRENAMIENTO));
		assertFalse(Permisos.puedeAcceder(Rol.GESTOR_INSTALACIONES, Funcionalidad.PROGRAMAR_JARDINERIA));
	}

	@Test
	void sinPermisoSeAvisaYNoSeEjecuta() {
		ApplicationException ex = assertThrows(ApplicationException.class,
				() -> Permisos.exigir(Rol.INVITADO, Funcionalidad.ALTA_EMPLEADOS));
		assertEquals("No tienes permiso para acceder a \"Alta de empleados\"", ex.getMessage());
	}

	@Test
	void conPermisoNoHayAviso() {
		Permisos.exigir(Rol.INVITADO, Funcionalidad.RESERVAR_INSTALACION);
		Permisos.exigir(Rol.GERENTE, Funcionalidad.ALTA_EMPLEADOS);
	}
}
