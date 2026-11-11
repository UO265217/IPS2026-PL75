package giis.ips.jardineria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import giis.ips.empleados.Posicion;
import giis.ips.entrenamientos.EntrenamientoModel;
import giis.ips.entrenamientos.UsoInstalacionDTO;
import giis.ips.reservas.FranjaDTO;
import giis.ips.reservas.InstalacionEntity;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio de la programacion de labores de jardineria en las instalaciones (HU 36125).
 * Una labor la hace un jardinero que debe estar de servicio ese dia y la labor debe quedar dentro de uno de sus
 * turnos (el horario de dia especifico, si lo hay, sustituye al periodico), sin coincidir con otra labor suya.
 * Tampoco puede coincidir con un entrenamiento, un partido, otra labor o una reserva externa en la instalacion.
 * Dos franjas pegadas (una acaba cuando empieza la otra) no coinciden.
 */
public class JardineriaModel {
	public static final int FIN_DIA = 24 * 60;

	private Database db = new Database();
	private LocalDate hoy;

	public JardineriaModel() {
		this(LocalDate.now());
	}

	/** Permite fijar el dia actual para que las pruebas no dependan de la fecha en que se ejecutan */
	public JardineriaModel(LocalDate hoy) {
		this.hoy = hoy;
	}

	/** Instalaciones ordenadas por nombre */
	public List<InstalacionEntity> getInstalaciones() {
		return db.executeQueryPojo(InstalacionEntity.class, "select id, nombre from instalacion order by nombre");
	}

	/** Empleados de jardineria, ordenados por apellido y nombre */
	public List<JardineroDTO> getJardineros() {
		return db.executeQueryPojo(JardineroDTO.class,
				"select id, nombre, apellido from empleado where posicion = ? order by apellido, nombre",
				Posicion.JARDINERIA.name());
	}

	/**
	 * Lo que ocupa la instalacion en la fecha (entrenamientos, partidos, reservas externas y labores), sin las labores
	 * del jardinero indicado, que se muestran con las suyas. Ordenado por hora de inicio
	 */
	public List<UsoInstalacionDTO> getUsosInstalacion(int instalacionId, LocalDate fecha, int jardineroId) {
		String sql = "select 'ENTRENAMIENTO' as tipo, en.id, eq.nombre as descripcion, en.horaInicio, en.horaFin"
				+ " from entrenamiento en join equipo eq on eq.id = en.equipoId where en.instalacionId = ? and en.fecha = ?"
				+ " union all select 'PARTIDO', p.id, eq.nombre, p.horaInicio, p.horaFin"
				+ " from partido p join equipo eq on eq.id = p.equipoId where p.instalacionId = ? and p.fecha = ?"
				+ " union all select 'RESERVA', id, nombreUsuario, horaInicio, horaFin"
				+ " from reserva where instalacionId = ? and fecha = ?"
				+ " union all select 'JARDINERIA', id, descripcion, horaInicio, horaFin"
				+ " from labor_jardineria where instalacionId = ? and fecha = ? and empleadoId <> ?"
				+ " order by horaInicio, horaFin";
		String f = fecha.toString();
		return db.executeQueryPojo(UsoInstalacionDTO.class, sql, instalacionId, f, instalacionId, f, instalacionId, f,
				instalacionId, f, jardineroId);
	}

	/** Labores del jardinero en la fecha, en cualquier instalacion, ordenadas por hora de inicio */
	public List<LaborJardineroDTO> getLaboresJardinero(int jardineroId, LocalDate fecha) {
		String sql = "select l.id, l.instalacionId, i.nombre as instalacion, l.descripcion, l.horaInicio, l.horaFin"
				+ " from labor_jardineria l join instalacion i on i.id = l.instalacionId"
				+ " where l.empleadoId = ? and l.fecha = ? order by l.horaInicio";
		return db.executeQueryPojo(LaborJardineroDTO.class, sql, jardineroId, fecha.toString());
	}

	/**
	 * Turnos del jardinero en la fecha, ordenados por hora: los de dia especifico si tiene alguno ese dia y, si no,
	 * los de su horario periodico para ese dia de la semana
	 */
	public List<FranjaDTO> getTurnos(int jardineroId, LocalDate fecha) {
		List<FranjaDTO> turnos = db.executeQueryPojo(FranjaDTO.class, "select horaInicio, horaFin"
				+ " from horario_especifico where empleadoId = ? and fecha = ? order by horaInicio", jardineroId,
				fecha.toString());
		if (!turnos.isEmpty())
			return turnos;
		return db.executeQueryPojo(FranjaDTO.class, "select horaInicio, horaFin from horario"
				+ " where empleadoId = ? and diaSemana = ? order by horaInicio", jardineroId,
				fecha.getDayOfWeek().getValue());
	}

	/** Usos de la instalacion que coinciden con la franja */
	public static List<UsoInstalacionDTO> getConflictosInstalacion(List<UsoInstalacionDTO> usos, int inicio, int fin) {
		List<UsoInstalacionDTO> conflictos = new ArrayList<>();
		for (UsoInstalacionDTO u : usos)
			if (seSolapan(inicio, fin, u.getHoraInicio(), u.getHoraFin()))
				conflictos.add(u);
		return conflictos;
	}

	/** Labores del jardinero que coinciden con la franja */
	public static List<LaborJardineroDTO> getConflictosJardinero(List<LaborJardineroDTO> labores, int inicio, int fin) {
		List<LaborJardineroDTO> conflictos = new ArrayList<>();
		for (LaborJardineroDTO l : labores)
			if (seSolapan(inicio, fin, l.getHoraInicio(), l.getHoraFin()))
				conflictos.add(l);
		return conflictos;
	}

	/**
	 * Motivo por el que no se puede programar una labor de inicio a fin, con los turnos y labores del jardinero y los
	 * usos de la instalacion de ese dia, o null si se puede. Comprueba, en este orden, las horas, que el jardinero
	 * trabaje ese dia, que la labor quede dentro de un turno, sus otras labores y los usos de la instalacion
	 */
	public static String motivoNoProgramable(int inicio, int fin, String jardinero, List<FranjaDTO> turnos,
			List<LaborJardineroDTO> labores, List<UsoInstalacionDTO> usos) {
		if (inicio < 0 || fin > FIN_DIA)
			return "Las horas deben estar entre las 00:00 y las 24:00";
		if (fin <= inicio)
			return "La hora de fin debe ser posterior a la de inicio";
		if (turnos.isEmpty())
			return jardinero + " no trabaja este día";
		boolean dentro = false;
		for (FranjaDTO t : turnos)
			dentro = dentro || t.getHoraInicio() <= inicio && fin <= t.getHoraFin();
		if (!dentro)
			return "La labor debe quedar dentro de un turno de " + jardinero + " (este día, " + describirTurnos(turnos)
					+ ")";
		List<LaborJardineroDTO> suyas = getConflictosJardinero(labores, inicio, fin);
		if (!suyas.isEmpty())
			return jardinero + " ya tiene otra labor " + suyas.get(0).describir();
		List<UsoInstalacionDTO> conflictos = getConflictosInstalacion(usos, inicio, fin);
		if (!conflictos.isEmpty())
			return "Coincide con " + conflictos.get(0).describir() + " en esta instalación";
		return null;
	}

	/** Turnos en formato "de 07:00 a 13:00 y de 16:00 a 18:00" */
	public static String describirTurnos(List<FranjaDTO> turnos) {
		StringBuilder sb = new StringBuilder();
		for (FranjaDTO t : turnos)
			sb.append(sb.length() == 0 ? "" : " y ").append("de ")
					.append(EntrenamientoModel.formatearHora(t.getHoraInicio())).append(" a ")
					.append(EntrenamientoModel.formatearHora(t.getHoraFin()));
		return sb.toString();
	}

	/** Guarda la labor tras comprobar todas las reglas de negocio; lanza ApplicationException si no se cumplen */
	public void programar(LaborJardineriaEntity labor) {
		validateCondition(labor.getDescripcion() != null && !labor.getDescripcion().trim().isEmpty(),
				"Hay que indicar la descripción de la labor");
		validateCondition(
				!db.executeQueryArray("select id from instalacion where id = ?", labor.getInstalacionId()).isEmpty(),
				"No existe la instalación seleccionada");
		List<Object[]> empleado = db.executeQueryArray("select nombre || ' ' || apellido, posicion from empleado"
				+ " where id = ?", labor.getEmpleadoId());
		validateCondition(!empleado.isEmpty(), "No existe el jardinero seleccionado");
		validateCondition(Posicion.JARDINERIA.name().equals(empleado.get(0)[1]),
				"El empleado seleccionado no es jardinero");
		validateCondition(labor.getFecha() != null && !labor.getFecha().isEmpty(), "Hay que indicar la fecha de la labor");
		LocalDate fecha = LocalDate.parse(labor.getFecha());
		validateCondition(!fecha.isBefore(hoy), "No se puede programar una labor en una fecha pasada");

		String motivo = motivoNoProgramable(labor.getHoraInicio(), labor.getHoraFin(), (String) empleado.get(0)[0],
				getTurnos(labor.getEmpleadoId(), fecha), getLaboresJardinero(labor.getEmpleadoId(), fecha),
				getUsosInstalacion(labor.getInstalacionId(), fecha, labor.getEmpleadoId()));
		validateCondition(motivo == null, motivo);

		db.executeUpdate("insert into labor_jardineria(instalacionId, empleadoId, descripcion, fecha, horaInicio, horaFin)"
				+ " values (?, ?, ?, ?, ?, ?)", labor.getInstalacionId(), labor.getEmpleadoId(),
				labor.getDescripcion().trim(), labor.getFecha(), labor.getHoraInicio(), labor.getHoraFin());
	}

	/** Dos franjas se solapan si cada una empieza antes de que acabe la otra (pegadas no se solapan) */
	private static boolean seSolapan(int inicio1, int fin1, int inicio2, int fin2) {
		return inicio1 < fin2 && inicio2 < fin1;
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
