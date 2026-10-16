package giis.ips.empleados;

import java.time.LocalDate;
import java.util.List;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio de la modificacion y eliminacion de empleados.
 * Recibe los datos ya validados sintacticamente por el controlador
 * y comprueba aqui las validaciones semanticas.
 */
public class GestionEmpleadosModel {

	private Database db = new Database();

	/** Lista de empleados del sistema, ordenados por apellido y nombre */
	public List<EmpleadoEntity> getEmpleados() {
		String sql = "select id, nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual"
				+ " from empleado order by apellido, nombre";
		return db.executeQueryPojo(EmpleadoEntity.class, sql);
	}

	/** Empleado con el id indicado; si no existe lanza una excepcion */
	public EmpleadoEntity getEmpleado(int id) {
		String sql = "select id, nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual"
				+ " from empleado where id = ?";
		List<EmpleadoEntity> empleados = db.executeQueryPojo(EmpleadoEntity.class, sql, id);
		validateCondition(!empleados.isEmpty(), "No existe el empleado seleccionado");
		return empleados.get(0);
	}

	/**
	 * Modifica los datos personales y profesionales de un empleado existente tras comprobar las reglas de negocio.
	 * El puesto de trabajo (tipo y posicion) no se puede cambiar.
	 * @param hoy fecha actual, como parametro para poder probar los limites de edad
	 */
	public void modificarEmpleado(EmpleadoEntity empleado, LocalDate hoy) {
		EmpleadoEntity actual = getEmpleado(empleado.getId());
		validateCondition(actual.getTipo().equals(empleado.getTipo())
				&& actual.getPosicion().equals(empleado.getPosicion()),
				"No se puede cambiar el puesto de trabajo de un empleado");
		LocalDate nacimiento = LocalDate.parse(empleado.getFechaNacimiento());
		validateCondition(!nacimiento.isAfter(hoy), "La fecha de nacimiento no puede ser posterior a hoy");
		validateCondition(AltaEmpleadoModel.esMayorDeEdad(nacimiento, hoy)
				|| Posicion.JUGADOR.name().equals(actual.getPosicion()),
				"Solo los jugadores pueden ser menores de edad");
		validateCondition(empleado.getSalarioAnual() > 0, "El salario bruto anual debe ser mayor que 0");
		validateCondition(!existeDniEnOtroEmpleado(empleado.getDni(), empleado.getId()),
				"Ya existe otro empleado con el DNI " + empleado.getDni());

		String sql = "update empleado set nombre = ?, apellido = ?, dni = ?, telefono = ?, fechaNacimiento = ?,"
				+ " salarioAnual = ? where id = ?";
		db.executeUpdate(sql, empleado.getNombre(), empleado.getApellido(), empleado.getDni(), empleado.getTelefono(),
				empleado.getFechaNacimiento(), empleado.getSalarioAnual(), empleado.getId());
	}

	/** Elimina del sistema al empleado con el id indicado */
	public void eliminarEmpleado(int id) {
		getEmpleado(id); // comprueba que existe
		db.executeUpdate("delete from horario where empleadoId = ?", id); // sus turnos (HU 35203)
		db.executeUpdate("delete from empleado where id = ?", id);
	}

	/** El DNI es unico: no puede tenerlo ningun empleado distinto del que se modifica */
	private boolean existeDniEnOtroEmpleado(String dni, int id) {
		List<Object[]> rows = db.executeQueryArray("select id from empleado where dni = ? and id <> ?", dni, id);
		return !rows.isEmpty();
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
