package giis.ips.entrenamientos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;

import giis.ips.reservas.InstalacionEntity;
import giis.ips.util.ApplicationException;
import giis.ips.util.Database;
import giis.ips.util.UnexpectedException;

/**
 * Acceso a datos y reglas de negocio de la asignacion de entrenamientos a instalaciones (HU 36124).
 * Un entrenamiento no puede coincidir con un partido ni con otro entrenamiento en la misma instalacion.
 * Tiene prioridad sobre las labores de jardineria y las reservas de usuarios externos: las que coincidan con el,
 * aunque sea en parte, se eliminan, y tambien las reservas externas que empiecen en la hora y media siguiente
 * a su final (en ese tiempo no se admiten reservas externas, HU 35206).
 */
public class EntrenamientoModel {
	public static final int MARGEN_TRAS_EQUIPO = 90;
	public static final int FIN_DIA = 24 * 60;

	private Database db = new Database();
	private LocalDate hoy;

	public EntrenamientoModel() {
		this(LocalDate.now());
	}

	/** Permite fijar el dia actual para que las pruebas no dependan de la fecha en que se ejecutan */
	public EntrenamientoModel(LocalDate hoy) {
		this.hoy = hoy;
	}

	/** Equipos ordenados por nombre */
	public List<EquipoEntity> getEquipos() {
		return db.executeQueryPojo(EquipoEntity.class, "select id, nombre from equipo order by nombre");
	}

	/** Instalaciones ordenadas por nombre */
	public List<InstalacionEntity> getInstalaciones() {
		return db.executeQueryPojo(InstalacionEntity.class, "select id, nombre from instalacion order by nombre");
	}

	/**
	 * Todo lo que ocupa la instalacion en la fecha indicada (entrenamientos, partidos, reservas externas y labores de
	 * jardineria), ordenado por hora de inicio
	 */
	public List<UsoInstalacionDTO> getUsos(int instalacionId, LocalDate fecha) {
		String sql = "select 'ENTRENAMIENTO' as tipo, en.id, eq.nombre as descripcion, en.horaInicio, en.horaFin"
				+ " from entrenamiento en join equipo eq on eq.id = en.equipoId where en.instalacionId = ? and en.fecha = ?"
				+ " union all select 'PARTIDO', p.id, eq.nombre, p.horaInicio, p.horaFin"
				+ " from partido p join equipo eq on eq.id = p.equipoId where p.instalacionId = ? and p.fecha = ?"
				+ " union all select 'RESERVA', id, nombreUsuario, horaInicio, horaFin"
				+ " from reserva where instalacionId = ? and fecha = ?"
				+ " union all select 'JARDINERIA', id, descripcion, horaInicio, horaFin"
				+ " from labor_jardineria where instalacionId = ? and fecha = ?"
				+ " order by horaInicio, horaFin";
		String f = fecha.toString();
		return db.executeQueryPojo(UsoInstalacionDTO.class, sql, instalacionId, f, instalacionId, f, instalacionId, f,
				instalacionId, f);
	}

	/** Usos de equipos (entrenamientos y partidos) que coinciden con la franja: impiden asignar el entrenamiento */
	public static List<UsoInstalacionDTO> getConflictos(List<UsoInstalacionDTO> usos, int horaInicio, int horaFin) {
		List<UsoInstalacionDTO> conflictos = new ArrayList<>();
		for (UsoInstalacionDTO u : usos)
			if (u.esDeEquipo() && seSolapan(horaInicio, horaFin, u.getHoraInicio(), u.getHoraFin()))
				conflictos.add(u);
		return conflictos;
	}

	/**
	 * Usos que se eliminan al asignar un entrenamiento en la franja: las labores de jardineria que coinciden con el,
	 * aunque sea en parte, y las reservas externas que coinciden con el o empiezan en la hora y media siguiente
	 */
	public static List<UsoInstalacionDTO> getAfectados(List<UsoInstalacionDTO> usos, int horaInicio, int horaFin) {
		List<UsoInstalacionDTO> afectados = new ArrayList<>();
		for (UsoInstalacionDTO u : usos) {
			boolean coincide = seSolapan(horaInicio, horaFin, u.getHoraInicio(), u.getHoraFin());
			if (UsoInstalacionDTO.JARDINERIA.equals(u.getTipo()) && coincide)
				afectados.add(u);
			else if (UsoInstalacionDTO.RESERVA.equals(u.getTipo()) && (coincide
					|| u.getHoraInicio() >= horaFin && u.getHoraInicio() < horaFin + MARGEN_TRAS_EQUIPO))
				afectados.add(u);
		}
		return afectados;
	}

	/** Reservas externas y labores de jardineria que se eliminarian al asignar el entrenamiento (no lo valida) */
	public List<UsoInstalacionDTO> getAfectados(EntrenamientoEntity e) {
		return getAfectados(getUsos(e.getInstalacionId(), LocalDate.parse(e.getFecha())), e.getHoraInicio(),
				e.getHoraFin());
	}

	/** Comprueba las reglas de negocio del entrenamiento sin guardar nada; lanza ApplicationException si no se cumplen */
	public void validar(EntrenamientoEntity e) {
		validateCondition(!db.executeQueryArray("select id from equipo where id = ?", e.getEquipoId()).isEmpty(),
				"No existe el equipo seleccionado");
		validateCondition(
				!db.executeQueryArray("select id from instalacion where id = ?", e.getInstalacionId()).isEmpty(),
				"No existe la instalación seleccionada");
		validateCondition(e.getFecha() != null && !e.getFecha().isEmpty(), "Hay que indicar la fecha del entrenamiento");
		LocalDate fecha = LocalDate.parse(e.getFecha());
		validateCondition(!fecha.isBefore(hoy), "No se puede asignar un entrenamiento en una fecha pasada");
		validateCondition(e.getHoraInicio() >= 0 && e.getHoraFin() <= FIN_DIA,
				"Las horas deben estar entre las 00:00 y las 24:00");
		validateCondition(e.getHoraFin() > e.getHoraInicio(), "La hora de fin debe ser posterior a la de inicio");

		List<UsoInstalacionDTO> conflictos = getConflictos(getUsos(e.getInstalacionId(), fecha), e.getHoraInicio(),
				e.getHoraFin());
		validateCondition(conflictos.isEmpty(), conflictos.isEmpty() ? ""
				: "No se puede asignar el entrenamiento: coincide con " + conflictos.get(0).describir()
						+ " en esta instalación");
	}

	/**
	 * Guarda el entrenamiento tras validarlo y elimina las reservas externas y labores de jardineria afectadas, todo
	 * en una misma transaccion. Devuelve lo que se ha eliminado
	 */
	public List<UsoInstalacionDTO> asignar(EntrenamientoEntity e) {
		validar(e);
		List<UsoInstalacionDTO> afectados = getAfectados(e);
		QueryRunner runner = new QueryRunner();
		try (Connection cn = db.getConnection()) {
			cn.setAutoCommit(false);
			try {
				for (UsoInstalacionDTO u : afectados)
					runner.update(cn, UsoInstalacionDTO.RESERVA.equals(u.getTipo()) ? "delete from reserva where id = ?"
							: "delete from labor_jardineria where id = ?", u.getId());
				runner.update(cn, "insert into entrenamiento(equipoId, instalacionId, fecha, horaInicio, horaFin)"
						+ " values (?, ?, ?, ?, ?)", e.getEquipoId(), e.getInstalacionId(), e.getFecha(),
						e.getHoraInicio(), e.getHoraFin());
				cn.commit();
			} catch (SQLException ex) {
				cn.rollback();
				throw ex;
			}
		} catch (SQLException ex) {
			throw new UnexpectedException(ex);
		}
		return afectados;
	}

	/** Dos franjas se solapan si cada una empieza antes de que acabe la otra (pegadas no se solapan) */
	private static boolean seSolapan(int inicio1, int fin1, int inicio2, int fin2) {
		return inicio1 < fin2 && inicio2 < fin1;
	}

	/** Minutos desde las 00:00 en formato HH:mm */
	public static String formatearHora(int minutos) {
		return String.format("%02d:%02d", minutos / 60, minutos % 60);
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
