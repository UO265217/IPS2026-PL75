package giis.ips.empleados;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio de los horarios periodicos de los empleados no deportivos (HU 35203)
 * y de su horario en una semana concreta del calendario (HU 35205).
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
		validateCondition(esNoDeportivo(turno.getEmpleadoId()), "Solo se puede asignar horario a empleados no deportivos");
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

	/** Lunes de la semana (de lunes a domingo) a la que pertenece la fecha */
	public static LocalDate getLunes(LocalDate fecha) {
		return fecha.with(DayOfWeek.MONDAY);
	}

	/**
	 * Turnos del empleado en la semana de la fecha indicada, ordenados por dia y hora de inicio.
	 * Si un dia tiene horario de dia especifico, este sustituye en su totalidad al periodico de ese dia;
	 * los turnos especificos se devuelven con el dia de la semana de su fecha y marcados como especificos.
	 */
	public List<HorarioEntity> getHorarioSemana(int empleadoId, LocalDate fecha) {
		validateCondition(esNoDeportivo(empleadoId), "Solo los empleados no deportivos tienen horario");
		LocalDate lunes = getLunes(fecha);
		String sql = "select id, empleadoId, fecha, horaInicio, horaFin from horario_especifico"
				+ " where empleadoId = ? and fecha between ? and ?";
		List<HorarioEspecificoEntity> especificos = db.executeQueryPojo(HorarioEspecificoEntity.class, sql,
				empleadoId, lunes.toString(), lunes.plusDays(6).toString());

		List<HorarioEntity> semana = new ArrayList<>();
		Set<Integer> diasEspecificos = new HashSet<>();
		for (HorarioEspecificoEntity e : especificos) {
			HorarioEntity h = new HorarioEntity();
			h.setId(e.getId());
			h.setEmpleadoId(e.getEmpleadoId());
			h.setDiaSemana(LocalDate.parse(e.getFecha()).getDayOfWeek().getValue());
			h.setHoraInicio(e.getHoraInicio());
			h.setHoraFin(e.getHoraFin());
			h.setEspecifico(true);
			semana.add(h);
			diasEspecificos.add(h.getDiaSemana());
		}
		for (HorarioEntity h : getHorarios(empleadoId))
			if (!diasEspecificos.contains(h.getDiaSemana()))
				semana.add(h);
		semana.sort(Comparator.comparingInt(HorarioEntity::getDiaSemana).thenComparingInt(HorarioEntity::getHoraInicio));
		return semana;
	}

	/** Comprueba que el empleado existe (si no lanza una excepcion) e indica si es no deportivo */
	private boolean esNoDeportivo(int empleadoId) {
		List<EmpleadoEntity> empleados = db.executeQueryPojo(EmpleadoEntity.class,
				"select id, tipo from empleado where id = ?", empleadoId);
		validateCondition(!empleados.isEmpty(), "No existe el empleado seleccionado");
		return TipoEmpleado.NO_DEPORTIVO.name().equals(empleados.get(0).getTipo());
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
