package giis.ips.reservas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio de las reservas de instalaciones por usuarios externos (HU 35206).
 * Las reservas van de las 8:00 a las 22:00, duran al menos 1 hora y pueden empezar en cualquier minuto.
 * No pueden coincidir con el uso de la instalacion por un equipo (entrenamientos y partidos) ni empezar antes
 * de que pase hora y media desde el final de ese uso, ni coincidir con otra reserva externa.
 * Se cobra en proporcion al tiempo reservado: 50 euros la hora (1 h 30 min son 75 euros).
 */
public class ReservaModel {
	public static final int APERTURA = 8 * 60;
	public static final int CIERRE = 22 * 60;
	public static final int DURACION_MINIMA = 60;
	public static final int MARGEN_TRAS_EQUIPO = 90;
	public static final int PRECIO_HORA = 50;

	private Database db = new Database();
	private LocalDate hoy;

	public ReservaModel() {
		this(LocalDate.now());
	}

	/** Permite fijar el dia actual para que las pruebas no dependan de la fecha en que se ejecutan */
	public ReservaModel(LocalDate hoy) {
		this.hoy = hoy;
	}

	/** Instalaciones ordenadas por nombre */
	public List<InstalacionEntity> getInstalaciones() {
		return db.executeQueryPojo(InstalacionEntity.class, "select id, nombre from instalacion order by nombre");
	}

	/** Reservas externas de la instalacion en la fecha indicada, ordenadas por hora de inicio */
	public List<ReservaEntity> getReservas(int instalacionId, LocalDate fecha) {
		String sql = "select id, instalacionId, fecha, horaInicio, horaFin, nombreUsuario, numeroTarjeta, importe"
				+ " from reserva where instalacionId = ? and fecha = ? order by horaInicio";
		return db.executeQueryPojo(ReservaEntity.class, sql, instalacionId, fecha.toString());
	}

	/** Usos de la instalacion por los equipos (entrenamientos y partidos) en la fecha indicada, sin el margen */
	public List<FranjaDTO> getUsosEquipos(int instalacionId, LocalDate fecha) {
		String sql = "select horaInicio, horaFin from entrenamiento where instalacionId = ? and fecha = ?"
				+ " union all select horaInicio, horaFin from partido where instalacionId = ? and fecha = ?";
		return db.executeQueryPojo(FranjaDTO.class, sql, instalacionId, fecha.toString(), instalacionId,
				fecha.toString());
	}

	/**
	 * Franjas de la fecha en las que se puede reservar la instalacion, ordenadas por hora: el horario de 8:00 a 22:00
	 * menos los usos de los equipos (alargados hora y media) y las reservas existentes.
	 * Solo se devuelven los huecos de al menos 1 hora, porque en los mas cortos no cabe ninguna reserva.
	 */
	public List<FranjaDTO> getFranjasLibres(int instalacionId, LocalDate fecha) {
		existeInstalacion(instalacionId);
		List<FranjaDTO> ocupadas = new ArrayList<>();
		for (FranjaDTO u : getUsosEquipos(instalacionId, fecha))
			ocupadas.add(new FranjaDTO(u.getHoraInicio(), u.getHoraFin() + MARGEN_TRAS_EQUIPO));
		for (ReservaEntity r : getReservas(instalacionId, fecha))
			ocupadas.add(new FranjaDTO(r.getHoraInicio(), r.getHoraFin()));
		ocupadas.sort(Comparator.comparingInt(FranjaDTO::getHoraInicio));

		List<FranjaDTO> libres = new ArrayList<>();
		int inicio = APERTURA;
		for (FranjaDTO o : ocupadas) {
			int fin = Math.min(o.getHoraInicio(), CIERRE);
			if (fin - inicio >= DURACION_MINIMA)
				libres.add(new FranjaDTO(inicio, fin));
			inicio = Math.max(inicio, o.getHoraFin());
		}
		if (CIERRE - inicio >= DURACION_MINIMA)
			libres.add(new FranjaDTO(inicio, CIERRE));
		return libres;
	}

	/**
	 * Importe de una reserva en proporcion a su duracion: 50 euros la hora (1 h 30 min son 75 euros),
	 * redondeado a centimos
	 */
	public static double calcularImporte(int horaInicio, int horaFin) {
		return Math.round((horaFin - horaInicio) * PRECIO_HORA * 100.0 / 60) / 100.0;
	}

	/**
	 * Guarda la reserva tras comprobar las reglas de negocio y calcula su importe, que se devuelve en la propia
	 * reserva (el importe que traiga se ignora)
	 */
	public void reservar(ReservaEntity reserva) {
		existeInstalacion(reserva.getInstalacionId());
		validateCondition(reserva.getNombreUsuario() != null && !reserva.getNombreUsuario().trim().isEmpty(),
				"Hay que indicar el nombre del usuario que solicita la reserva");
		validateCondition(reserva.getNumeroTarjeta() != null && !reserva.getNumeroTarjeta().trim().isEmpty(),
				"Hay que indicar el número de tarjeta bancaria para el cobro");
		LocalDate fecha = LocalDate.parse(reserva.getFecha());
		validateCondition(!fecha.isBefore(hoy), "No se puede reservar en una fecha pasada");
		String motivo = motivoNoReservable(reserva.getHoraInicio(), reserva.getHoraFin(),
				getUsosEquipos(reserva.getInstalacionId(), fecha), getReservas(reserva.getInstalacionId(), fecha));
		validateCondition(motivo == null, motivo);

		reserva.setImporte(calcularImporte(reserva.getHoraInicio(), reserva.getHoraFin()));
		db.executeUpdate("insert into reserva(instalacionId, fecha, horaInicio, horaFin, nombreUsuario, numeroTarjeta,"
				+ " importe) values (?, ?, ?, ?, ?, ?, ?)", reserva.getInstalacionId(), reserva.getFecha(),
				reserva.getHoraInicio(), reserva.getHoraFin(), reserva.getNombreUsuario().trim(),
				reserva.getNumeroTarjeta().trim(), reserva.getImporte());
	}

	/**
	 * Motivo por el que no se puede reservar de inicio a fin con los usos de los equipos y las reservas indicados,
	 * o null si se puede. Comprueba la duracion, el horario, los usos (con su hora y media posterior) y las reservas
	 */
	public static String motivoNoReservable(int inicio, int fin, List<FranjaDTO> usos, List<ReservaEntity> reservas) {
		if (fin - inicio < DURACION_MINIMA)
			return "La reserva debe durar al menos 1 hora";
		if (inicio < APERTURA || fin > CIERRE)
			return "Solo se puede reservar entre las 8:00 y las 22:00";
		for (FranjaDTO u : usos) {
			if (seSolapan(inicio, fin, u.getHoraInicio(), u.getHoraFin()))
				return "La instalación está ocupada por un equipo en esa franja";
			if (seSolapan(inicio, fin, u.getHoraFin(), u.getHoraFin() + MARGEN_TRAS_EQUIPO))
				return "No se puede reservar hasta pasada 1 hora y media desde que termina el uso de un equipo";
		}
		for (ReservaEntity r : reservas)
			if (seSolapan(inicio, fin, r.getHoraInicio(), r.getHoraFin()))
				return "La instalación ya tiene otra reserva en esa franja";
		return null;
	}

	/** Dos franjas se solapan si cada una empieza antes de que acabe la otra (pegadas no se solapan) */
	public static boolean seSolapan(int inicio1, int fin1, int inicio2, int fin2) {
		return inicio1 < fin2 && inicio2 < fin1;
	}

	private void existeInstalacion(int instalacionId) {
		validateCondition(!db.executeQueryArray("select id from instalacion where id = ?", instalacionId).isEmpty(),
				"No existe la instalación seleccionada");
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
