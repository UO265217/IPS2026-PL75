package giis.ips.login;

/**
 * Funcionalidades de la aplicacion a las que se puede dar o negar el acceso segun el rol (HU 37116).
 * Hay una por cada opcion del menu principal
 */
public enum Funcionalidad {
	ALTA_EMPLEADOS("Alta de empleados"),
	GESTION_EMPLEADOS("Modificar y eliminar empleados"),
	HORARIOS_EMPLEADOS("Horarios de empleados"),
	MI_HORARIO_SEMANAL("Ver mi horario semanal"),
	RESERVAR_INSTALACION("Reservar instalación"),
	ASIGNAR_ENTRENAMIENTO("Asignar entrenamiento a instalación"),
	PROGRAMAR_JARDINERIA("Programar labor de jardinería");

	private final String nombre;

	Funcionalidad(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}
}
