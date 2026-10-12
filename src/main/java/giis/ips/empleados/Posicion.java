package giis.ips.empleados;

import java.util.ArrayList;
import java.util.List;

/**
 * Posiciones que puede ocupar un empleado, cada una asociada a su tipo de empleado
 */
public enum Posicion {
	JUGADOR("Jugador", TipoEmpleado.DEPORTIVO),
	ENTRENADOR("Entrenador", TipoEmpleado.DEPORTIVO),
	GERENTE("Gerente", TipoEmpleado.NO_DEPORTIVO),
	VENDEDOR_ENTRADAS("Vendedor de entradas/abonos", TipoEmpleado.NO_DEPORTIVO),
	ENCARGADO_TIENDA("Encargado de tienda", TipoEmpleado.NO_DEPORTIVO),
	GESTOR_INSTALACIONES("Gestor de instalaciones", TipoEmpleado.NO_DEPORTIVO),
	EMPLEADO_TIENDA("Empleado de tienda", TipoEmpleado.NO_DEPORTIVO),
	JARDINERIA("Jardinería", TipoEmpleado.NO_DEPORTIVO),
	COCINA("Cocina", TipoEmpleado.NO_DEPORTIVO),
	DIRECTOR_COMUNICACIONES("Director de comunicaciones", TipoEmpleado.NO_DEPORTIVO);

	private final String nombre;
	private final TipoEmpleado tipo;

	Posicion(String nombre, TipoEmpleado tipo) {
		this.nombre = nombre;
		this.tipo = tipo;
	}

	public String getNombre() {
		return nombre;
	}

	public TipoEmpleado getTipo() {
		return tipo;
	}

	/** Nombres de las posiciones de un tipo, en el orden en que se muestran */
	public static List<String> nombresDeTipo(TipoEmpleado tipo) {
		List<String> nombres = new ArrayList<>();
		for (Posicion p : values())
			if (p.tipo == tipo)
				nombres.add(p.nombre);
		return nombres;
	}

	/** Busca la posicion por el nombre que se muestra en la interfaz */
	public static Posicion fromNombre(String nombre) {
		for (Posicion p : values())
			if (p.nombre.equals(nombre))
				return p;
		return null;
	}
}
