package giis.ips.login;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido del menu principal segun el rol (HU 37120): que tipo de menu se muestra, que accesos tiene y los textos
 * de cabecera. No depende de Swing para poder probarlo
 */
public final class MenuPrincipal {

	/** Los tres menus diferenciados */
	public enum Tipo {
		GERENTE, EMPLEADO, EXTERNO
	}

	public static final String MENSAJE_SIN_OPCIONES = "No tienes funcionalidades asignadas para tu puesto";

	private MenuPrincipal() {
	}

	public static Tipo tipoDe(Rol rol) {
		if (rol == Rol.GERENTE)
			return Tipo.GERENTE;
		if (rol.esInvitado())
			return Tipo.EXTERNO;
		return Tipo.EMPLEADO;
	}

	/** Accesos que muestra el menu del rol, en el orden de {@link Funcionalidad} y solo los permitidos */
	public static List<Funcionalidad> opciones(Rol rol) {
		List<Funcionalidad> opciones = new ArrayList<>();
		for (Funcionalidad f : Funcionalidad.values())
			if (Permisos.puedeAcceder(rol, f))
				opciones.add(f);
		return opciones;
	}

	/** Si el rol no tiene ningun acceso, el menu muestra {@link #MENSAJE_SIN_OPCIONES} */
	public static boolean sinOpciones(Rol rol) {
		return opciones(rol).isEmpty();
	}

	/** Nombre de usuario de la sesion o "Invitado" si entro sin cuenta */
	public static String nombreMostrado(Sesion sesion) {
		return sesion.esInvitado() ? "Invitado" : sesion.getNombreUsuario();
	}

	/** Nombre legible del rol: el de su posicion o "Usuario externo" para el invitado */
	public static String nombreRol(Rol rol) {
		return rol.esInvitado() ? "Usuario externo" : rol.getPosicion().getNombre();
	}

	public static String textoSalida(Sesion sesion) {
		return sesion.esInvitado() ? "Salir del modo invitado" : "Cerrar sesión";
	}
}
