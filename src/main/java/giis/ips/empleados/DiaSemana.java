package giis.ips.empleados;

import java.util.ArrayList;
import java.util.List;

/**
 * Dias de la semana de un turno de horario; el numero (1 lunes ... 7 domingo) es el que se guarda en la base de datos
 */
public enum DiaSemana {
	LUNES(1, "Lunes"),
	MARTES(2, "Martes"),
	MIERCOLES(3, "Miércoles"),
	JUEVES(4, "Jueves"),
	VIERNES(5, "Viernes"),
	SABADO(6, "Sábado"),
	DOMINGO(7, "Domingo");

	private final int numero;
	private final String nombre;

	DiaSemana(int numero, String nombre) {
		this.numero = numero;
		this.nombre = nombre;
	}

	public int getNumero() {
		return numero;
	}

	public String getNombre() {
		return nombre;
	}

	/** Nombres de los dias, en el orden en que se muestran */
	public static List<String> nombres() {
		List<String> nombres = new ArrayList<>();
		for (DiaSemana d : values())
			nombres.add(d.nombre);
		return nombres;
	}

	/** Busca el dia por el nombre que se muestra en la interfaz */
	public static DiaSemana fromNombre(String nombre) {
		for (DiaSemana d : values())
			if (d.nombre.equals(nombre))
				return d;
		return null;
	}

	/** Busca el dia por su numero (1 lunes ... 7 domingo) */
	public static DiaSemana fromNumero(int numero) {
		for (DiaSemana d : values())
			if (d.numero == numero)
				return d;
		return null;
	}
}
