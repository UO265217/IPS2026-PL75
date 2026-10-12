package giis.ips.empleados;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio del alta de empleados.
 * Recibe los datos ya validados sintacticamente por el controlador
 * y comprueba aqui las validaciones semanticas.
 */
public class AltaEmpleadoModel {
	public static final int MAYORIA_EDAD = 18;

	private Database db = new Database();

	/**
	 * Da de alta un empleado tras comprobar las reglas de negocio.
	 * @param hoy fecha actual, como parametro para poder probar los limites de edad
	 */
	public void altaEmpleado(EmpleadoEntity empleado, LocalDate hoy) {
		Posicion posicion = validarPosicion(empleado);
		LocalDate nacimiento = LocalDate.parse(empleado.getFechaNacimiento());
		validateCondition(!nacimiento.isAfter(hoy), "La fecha de nacimiento no puede ser posterior a hoy");
		validateCondition(esMayorDeEdad(nacimiento, hoy) || posicion == Posicion.JUGADOR,
				"Solo se pueden dar de alta menores de edad como jugadores");
		validateCondition(empleado.getSalarioAnual() > 0, "El salario bruto anual debe ser mayor que 0");
		validateCondition(!existeDni(empleado.getDni()), "Ya existe un empleado con el DNI " + empleado.getDni());

		String sql = "insert into empleado(nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual)"
				+ " values (?, ?, ?, ?, ?, ?, ?, ?)";
		db.executeUpdate(sql, empleado.getNombre(), empleado.getApellido(), empleado.getDni(), empleado.getTelefono(),
				empleado.getFechaNacimiento(), empleado.getTipo(), empleado.getPosicion(), empleado.getSalarioAnual());
	}

	/** Lista de empleados dados de alta, ordenados por apellido y nombre */
	public List<EmpleadoEntity> getEmpleados() {
		String sql = "select id, nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual"
				+ " from empleado order by apellido, nombre";
		return db.executeQueryPojo(EmpleadoEntity.class, sql);
	}

	public boolean existeDni(String dni) {
		List<Object[]> rows = db.executeQueryArray("select id from empleado where dni = ?", dni);
		return !rows.isEmpty();
	}

	public static boolean esMayorDeEdad(LocalDate nacimiento, LocalDate hoy) {
		return Period.between(nacimiento, hoy).getYears() >= MAYORIA_EDAD;
	}

	/** La posicion debe existir y corresponder al tipo de empleado indicado */
	private Posicion validarPosicion(EmpleadoEntity empleado) {
		Posicion posicion;
		TipoEmpleado tipo;
		try {
			posicion = Posicion.valueOf(empleado.getPosicion());
			tipo = TipoEmpleado.valueOf(empleado.getTipo());
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new ApplicationException("Tipo de empleado o posición no válidos");
		}
		validateCondition(posicion.getTipo() == tipo,
				"La posición " + posicion.getNombre() + " no corresponde a un empleado " + tipo.getNombre().toLowerCase());
		return posicion;
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
