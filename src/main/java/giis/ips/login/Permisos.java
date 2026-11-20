package giis.ips.login;

import java.util.EnumSet;
import java.util.Set;

/**
 * Que puede hacer cada rol (HU 37116). El gerente accede a todo; el invitado solo puede reservar instalaciones;
 * el resto de roles solo ven su horario y, en su caso, la funcionalidad propia de su puesto
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
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.RESERVAR_INSTALACION,
					Funcionalidad.ASIGNAR_ENTRENAMIENTO, Funcionalidad.PROGRAMAR_JARDINERIA);
		case JARDINERIA:
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL, Funcionalidad.PROGRAMAR_JARDINERIA);
		default:
			return EnumSet.of(Funcionalidad.MI_HORARIO_SEMANAL);
		}
	}

	public static boolean puedeAcceder(Rol rol, Funcionalidad funcionalidad) {
		return deRol(rol).contains(funcionalidad);
	}
}
