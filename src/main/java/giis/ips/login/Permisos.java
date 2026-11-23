package giis.ips.login;

import java.util.EnumSet;
import java.util.Set;

import giis.ips.util.ApplicationException;

/**
 * Que puede hacer cada rol (HU 37116). El gerente accede a todo; el invitado (usuario externo) solo puede reservar
 * instalaciones; cada empleado accede a las funcionalidades de su posicion (el gestor de instalaciones a las reservas,
 * el entrenador a los entrenamientos, jardineria a sus labores) y a ver su propio horario
 */
public final class Permisos {

	private Permisos() {
	}

	/** Funcionalidades a las que tiene acceso un rol */
	public static Set<Funcionalidad> deRol(Rol rol) {
		switch (rol) {
		case GERENTE:
			return EnumSet.allOf(Funcionalidad.class);
		case INVITADO:
			return EnumSet.of(Funcionalidad.RESERVAR_INSTALACION);
		case ENTRENADOR:
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.ASIGNAR_ENTRENAMIENTO);
		case GESTOR_INSTALACIONES:
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.RESERVAR_INSTALACION);
		case JARDINERIA:
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.PROGRAMAR_JARDINERIA);
		default:
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL);
		}
	}

	public static boolean puedeAcceder(Rol rol, Funcionalidad funcionalidad) {
		return deRol(rol).contains(funcionalidad);
	}

	/** Comprobacion previa a ejecutar una funcionalidad: si el rol no tiene acceso lanza un aviso y no se hace nada */
	public static void exigir(Rol rol, Funcionalidad funcionalidad) {
		if (!puedeAcceder(rol, funcionalidad))
			throw new ApplicationException("No tienes permiso para acceder a \"" + funcionalidad.getNombre() + "\"");
	}
}
