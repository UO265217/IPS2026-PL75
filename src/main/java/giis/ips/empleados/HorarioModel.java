package giis.ips.empleados;

import java.util.List;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio de los horarios periodicos de los empleados no deportivos.
 * Recibe los turnos ya validados sintacticamente por el controlador
 * y comprueba aqui las validaciones semanticas.
 */
public class HorarioModel {
	public static final int MINUTOS_DIA = 8 * 60;
	public static final int MINUTOS_SEMANA = 40 * 60;

	private Database db = new Database();

	/** Empleados no deportivos, ordenados por apellido y nombre */
	public List<EmpleadoEntity> getEmpleadosNoDeportivos() {
		String sql = "select id, nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual"
				+ " from empleado where tipo = ? order by apellido, nombre";
		return db.executeQueryPojo(EmpleadoEntity.class, sql, TipoEmpleado.NO_DEPORTIVO.name());
	}

	/** Turnos del empleado, ordenados por dia y hora de inicio */
	public List<HorarioEntity> getHorarios(int empleadoId) {
		String sql = "select id, empleadoId, diaSemana, horaInicio, horaFin from horario"
				+ " where empleadoId = ? order by diaSemana, horaInicio";
		return db.executeQueryPojo(HorarioEntity.class, sql, empleadoId);
	}

	/**
	 * Anyade un turno al horario semanal tras comprobar las reglas de negocio:
	 * empleado no deportivo, turno dentro del dia, sin solapes y maximo 8 h diarias y 40 h semanales.
	 */
	public void anyadirTurno(HorarioEntity turno) {
		List<EmpleadoEntity> empleados = db.executeQueryPojo(EmpleadoEntity.class,
				"select id, tipo from empleado where id = ?", turno.getEmpleadoId());
		validateCondition(!empleados.isEmpty(), "No existe el empleado seleccionado");
		validateCondition(TipoEmpleado.NO_DEPORTIVO.name().equals(empleados.get(0).getTipo()),
				"Solo se puede asignar horario a empleados no deportivos");
		validateCondition(turno.getHoraInicio() < turno.getHoraFin(),
				"La hora de inicio debe ser anterior a la hora de fin");
		validateCondition(turno.getHoraInicio() >= 0 && turno.getHoraFin() <= 24 * 60,
				"El turno debe estar dentro del mismo día (de 00:00 a 24:00)");

		List<HorarioEntity> existentes = getHorarios(turno.getEmpleadoId());
		int minutosDia = 0;
		int minutosSemana = 0;
		for (HorarioEntity h : existentes) {
			minutosSemana += h.getHoraFin() - h.getHoraInicio();
			if (h.getDiaSemana() == turno.getDiaSemana()) {
				validateCondition(turno.getHoraInicio() >= h.getHoraFin() || turno.getHoraFin() <= h.getHoraInicio(),
						"El turno se solapa con otro turno del mismo día");
				minutosDia += h.getHoraFin() - h.getHoraInicio();
			}
		}
		int duracion = turno.getHoraFin() - turno.getHoraInicio();
		validateCondition(minutosDia + duracion <= MINUTOS_DIA, "No se pueden superar las 8 horas de trabajo diarias");
		validateCondition(minutosSemana + duracion <= MINUTOS_SEMANA,
				"No se pueden superar las 40 horas de trabajo semanales");

		db.executeUpdate("insert into horario(empleadoId, diaSemana, horaInicio, horaFin) values (?, ?, ?, ?)",
				turno.getEmpleadoId(), turno.getDiaSemana(), turno.getHoraInicio(), turno.getHoraFin());
	}

	/** Elimina el turno con el id indicado; si no existe lanza una excepcion */
	public void eliminarTurno(int id) {
		validateCondition(!db.executeQueryArray("select id from horario where id = ?", id).isEmpty(),
				"No existe el turno seleccionado");
		db.executeUpdate("delete from horario where id = ?", id);
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
