package giis.ips.empleados;

/**
 * Tipos de empleado: deportivos (jugadores y entrenadores) y no deportivos (resto de la plantilla)
 */
public enum TipoEmpleado {
	DEPORTIVO("Deportivo"),
	NO_DEPORTIVO("No deportivo");

	private final String nombre;

	TipoEmpleado(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}

	/** Busca el tipo por el nombre que se muestra en la interfaz */
	public static TipoEmpleado fromNombre(String nombre) {
		for (TipoEmpleado t : values())
			if (t.nombre.equals(nombre))
				return t;
		return null;
	}
}
