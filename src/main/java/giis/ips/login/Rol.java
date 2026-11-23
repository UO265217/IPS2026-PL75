package giis.ips.login;

import giis.ips.empleados.Posicion;

/**
 * Roles de acceso (HU 37116): uno por cada posicion de empleado y el invitado, que entra sin cuenta.
 * El rol de una cuenta es la posicion del empleado al que pertenece
 */
public enum Rol {
	JUGADOR(Posicion.JUGADOR),
	ENTRENADOR(Posicion.ENTRENADOR),
	GERENTE(Posicion.GERENTE),
	VENDEDOR_ENTRADAS(Posicion.VENDEDOR_ENTRADAS),
	ENCARGADO_TIENDA(Posicion.ENCARGADO_TIENDA),
	GESTOR_INSTALACIONES(Posicion.GESTOR_INSTALACIONES),
	EMPLEADO_TIENDA(Posicion.EMPLEADO_TIENDA),
	JARDINERIA(Posicion.JARDINERIA),
	COCINA(Posicion.COCINA),
	DIRECTOR_COMUNICACIONES(Posicion.DIRECTOR_COMUNICACIONES),
	INVITADO(null);

	private final Posicion posicion;

	Rol(Posicion posicion) {
		this.posicion = posicion;
	}

	/** Posicion de empleado de este rol; null para el invitado */
	public Posicion getPosicion() {
		return posicion;
	}

	public boolean esInvitado() {
		return this == INVITADO;
	}

	/** Rol de un empleado a partir del nombre de su posicion (como se guarda en la tabla empleado); null si no existe */
	public static Rol deEmpleado(String nombrePosicion) {
		for (Rol r : values())
			if (r.posicion != null && r.posicion.name().equals(nombrePosicion))
				return r;
		return null;
	}
}
