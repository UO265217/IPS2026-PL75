package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import giis.ips.login.Funcionalidad;
import giis.ips.login.MenuPrincipal;
import giis.ips.login.MenuPrincipal.Tipo;
import giis.ips.login.Rol;
import giis.ips.login.Sesion;

/**
 * Pruebas del menu principal segun el rol (HU 37120)
 */
class TestMenuPrincipal {

	@Test
	void cadaRolTieneSuTipoDeMenu() {
		assertEquals(Tipo.GERENTE, MenuPrincipal.tipoDe(Rol.GERENTE));
		assertEquals(Tipo.EXTERNO, MenuPrincipal.tipoDe(Rol.INVITADO));
		for (Rol r : Rol.values())
			if (r != Rol.GERENTE && r != Rol.INVITADO)
				assertEquals(Tipo.EMPLEADO, MenuPrincipal.tipoDe(r), r.name());
	}

	@Test
	void elGerenteVeTodasLasFuncionalidades() {
		assertEquals(Arrays.asList(Funcionalidad.values()), MenuPrincipal.opciones(Rol.GERENTE));
	}

	@Test
	void elExternoSoloVeReservar() {
		assertEquals(List.of(Funcionalidad.RESERVAR_INSTALACION), MenuPrincipal.opciones(Rol.INVITADO));
	}

	@Test
	void elEmpleadoSoloVeLasDeSuPosicion() {
		assertEquals(List.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.ASIGNAR_ENTRENAMIENTO),
				MenuPrincipal.opciones(Rol.ENTRENADOR));
		assertEquals(List.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.PROGRAMAR_JARDINERIA),
				MenuPrincipal.opciones(Rol.JARDINERIA));
	}

	@Test
	void ningunMenuMuestraAccesosNoPermitidos() {
		for (Rol r : Rol.values())
			for (Funcionalidad f : MenuPrincipal.opciones(r))
				assertTrue(giis.ips.login.Permisos.puedeAcceder(r, f), r + " no deberia ver " + f);
	}

	@Test
	void elEmpleadoSinFuncionalidadesPropiasVeSoloSuHorario() {
		assertEquals(List.of(Funcionalidad.MI_HORARIO_SEMANAL), MenuPrincipal.opciones(Rol.COCINA));
		assertFalse(MenuPrincipal.sinOpciones(Rol.COCINA));
	}

	@Test
	void ningunRolDejaElMenuVacioSalvoQueNoTengaNada() {
		for (Rol r : Rol.values())
			assertEquals(MenuPrincipal.opciones(r).isEmpty(), MenuPrincipal.sinOpciones(r), r.name());
	}

	@Test
	void elMensajeDeSinOpcionesExplicaLaSituacion() {
		assertEquals("No tienes funcionalidades asignadas para tu puesto", MenuPrincipal.MENSAJE_SIN_OPCIONES);
	}

	@Test
	void elNombreMostradoEsElUsuarioOInvitado() {
		assertEquals("Invitado", MenuPrincipal.nombreMostrado(new Sesion(Rol.INVITADO, null, 0)));
		assertEquals("ana", MenuPrincipal.nombreMostrado(new Sesion(Rol.GERENTE, "ana", 3)));
	}

	@Test
	void lasEtiquetasDelRolSonLegibles() {
		assertEquals("Gerente", MenuPrincipal.nombreRol(Rol.GERENTE));
		assertEquals("Gestor de instalaciones", MenuPrincipal.nombreRol(Rol.GESTOR_INSTALACIONES));
		assertEquals("Usuario externo", MenuPrincipal.nombreRol(Rol.INVITADO));
	}

	@Test
	void elBotonDeSalidaDependeDeSiHayCuenta() {
		assertEquals("Salir del modo invitado", MenuPrincipal.textoSalida(new Sesion(Rol.INVITADO, null, 0)));
		assertEquals("Cerrar sesión", MenuPrincipal.textoSalida(new Sesion(Rol.GERENTE, "ana", 3)));
	}
}
