package giis.ips.empleados;

import java.awt.CardLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador de los horarios de empleados no deportivos: segun el tipo de horario elegido se editan
 * los turnos periodicos de cada dia de la semana (HU 35203) o los de un dia concreto del calendario (HU 35204).
 * Instala los manejadores de la vista, realiza las validaciones sintacticas y llama al modelo.
 */
public class HorarioController {
	private static final String[] COLUMNAS_EMPLEADOS = { "Id", "Apellido", "Nombre", "Posición" };
	private static final Pattern HORA = Pattern.compile("(\\d{2}):(\\d{2})");
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private HorarioModel model;
	private HorarioView view;
	private EmpleadoEntity seleccionado; // empleado cuyo horario se muestra o null si no hay ninguno

	public HorarioController(HorarioModel m, HorarioView v) {
		this.model = m;
		this.view = v;
		this.initView();
	}

	/** Inicializacion del controlador: anyade los manejadores de eventos */
	public void initController() {
		view.getTabEmpleados().getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
			if (!e.getValueIsAdjusting())
				SwingUtil.exceptionWrapper(() -> seleccionarEmpleado());
		});
		view.getCbTipo().addActionListener(e -> SwingUtil.exceptionWrapper(() -> mostrarHorario()));
		// Cualquier cambio de fecha (en el selector o con las flechas) vuelve a mostrar el dia y su semana
		view.getDcFecha().addPropertyChangeListener("date", e -> SwingUtil.exceptionWrapper(() -> mostrarHorario()));
		view.getBtnAnterior().addActionListener(e -> SwingUtil.exceptionWrapper(() -> moverDia(-1)));
		view.getBtnSiguiente().addActionListener(e -> SwingUtil.exceptionWrapper(() -> moverDia(1)));
		view.getBtnAnyadir().addActionListener(e -> SwingUtil.exceptionWrapper(() -> anyadir()));
		view.getBtnEliminar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> eliminar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		view.getCbDia().setModel(new DefaultComboBoxModel<>(DiaSemana.nombres().toArray(new String[0])));
		SwingUtil.setIsoDate(view.getDcFecha(), LocalDate.now().toString());
		cargarEmpleados();
		mostrarHorario();
		view.getFrame().setVisible(true);
	}

	/** Carga la tabla de empleados no deportivos; la primera columna es el id (como String) para usarlo de clave */
	public void cargarEmpleados() {
		DefaultTableModel tm = new DefaultTableModel(COLUMNAS_EMPLEADOS, 0);
		for (EmpleadoEntity e : model.getEmpleadosNoDeportivos())
			tm.addRow(new Object[] { String.valueOf(e.getId()), e.getApellido(), e.getNombre(),
					Posicion.valueOf(e.getPosicion()).getNombre() });
		view.getTabEmpleados().setModel(tm);
		ocultarId(view.getTabEmpleados());
	}

	/** Quita de la vista la columna del id (sigue en el modelo como clave) y reparte el ancho entre las demas */
	private static void ocultarId(JTable tabla) {
		tabla.removeColumn(tabla.getColumnModel().getColumn(0));
		tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	public void seleccionarEmpleado() {
		String key = SwingUtil.getSelectedKey(view.getTabEmpleados());
		seleccionado = key.isEmpty() ? null
				: model.getEmpleadosNoDeportivos().stream()
						.filter(e -> String.valueOf(e.getId()).equals(key)).findFirst().orElse(null);
		mostrarHorario();
	}

	/** Indica si se esta editando el horario de dia especifico (si no, el periodico) */
	private boolean esEspecifico() {
		return HorarioView.TIPO_ESPECIFICO.equals(view.getCbTipo().getSelectedItem());
	}

	/** Mueve la fecha seleccionada el numero de dias indicado (si no hay ninguna, parte de hoy) */
	public void moverDia(int dias) {
		SwingUtil.setIsoDate(view.getDcFecha(), getFecha().plusDays(dias).toString());
	}

	/** Fecha del selector o, si se ha borrado, la de hoy */
	private LocalDate getFecha() {
		String iso = SwingUtil.getIsoDate(view.getDcFecha());
		return iso.isEmpty() ? LocalDate.now() : LocalDate.parse(iso);
	}

	/**
	 * Muestra el horario del empleado segun el tipo elegido: el periodico, o la semana del dia seleccionado
	 * (resaltando ese dia) con las horas que lleva ese dia y esa semana. Los botones solo se activan si hay empleado.
	 */
	private void mostrarHorario() {
		boolean especifico = esEspecifico();
		((CardLayout) view.getPanelDia().getLayout()).show(view.getPanelDia(),
				especifico ? HorarioView.TIPO_ESPECIFICO : HorarioView.TIPO_PERIODICO);
		boolean hay = seleccionado != null;
		if (especifico)
			mostrarSemana(hay);
		else
			mostrarPeriodico(hay);
		view.getBtnAnyadir().setEnabled(hay);
		view.getBtnEliminar().setEnabled(hay);
	}

	private void mostrarPeriodico(boolean hay) {
		view.getLblHorario().setText("Horario semanal por defecto (pulsa un turno para seleccionarlo):");
		view.getPanelHorario().setLunes(null);
		view.getPanelHorario().setMarcado(null);
		List<HorarioEntity> turnos = hay ? model.getHorarios(seleccionado.getId()) : null;
		int totalMinutos = 0;
		if (hay)
			for (HorarioEntity h : turnos)
				totalMinutos += h.getHoraFin() - h.getHoraInicio();
		view.getPanelHorario().setTurnos(turnos);
		view.getLblTotal().setText(hay ? "Total semanal: " + formatearHora(totalMinutos) + " de 40:00 horas" : " ");
	}

	private void mostrarSemana(boolean hay) {
		LocalDate fecha = getFecha();
		LocalDate lunes = HorarioModel.getLunes(fecha);
		view.getLblHorario().setText("Semana del " + lunes.format(FECHA) + " al " + lunes.plusDays(6).format(FECHA)
				+ " (en naranja los turnos de día específico; resaltado el día " + fecha.format(FECHA) + ")");
		view.getPanelHorario().setLunes(lunes);
		view.getPanelHorario().setMarcado(fecha);
		List<HorarioEntity> turnos = hay ? model.getHorarioSemana(seleccionado.getId(), fecha) : null;
		int minutosDia = 0;
		int minutosSemana = 0;
		if (hay)
			for (HorarioEntity h : turnos) {
				minutosSemana += h.getHoraFin() - h.getHoraInicio();
				if (h.getDiaSemana() == fecha.getDayOfWeek().getValue())
					minutosDia += h.getHoraFin() - h.getHoraInicio();
			}
		view.getPanelHorario().setTurnos(turnos);
		view.getLblTotal().setText(hay ? "Día " + fecha.format(FECHA) + ": " + formatearHora(minutosDia)
				+ " de 08:00 horas · Semana: " + formatearHora(minutosSemana) + " de 40:00 horas" : " ");
	}

	public void anyadir() {
		if (seleccionado == null)
			throw new ApplicationException("Hay que seleccionar un empleado");
		String inicio = leerHora(view.getSpHoraInicio(), view.getSpMinInicio());
		String fin = leerHora(view.getSpHoraFin(), view.getSpMinFin());
		if (esEspecifico())
			model.anyadirTurnoEspecifico(
					crearTurnoEspecifico(seleccionado.getId(), SwingUtil.getIsoDate(view.getDcFecha()), inicio, fin));
		else
			model.anyadirTurno(crearTurno(seleccionado.getId(), (String) view.getCbDia().getSelectedItem(), inicio, fin));
		mostrarHorario();
	}

	/** Hora de los spinners en formato HH:mm, para validarla igual que el resto de horas */
	private static String leerHora(JSpinner spHora, JSpinner spMin) {
		return formatearHora((Integer) spHora.getValue() * 60 + (Integer) spMin.getValue());
	}

	public void eliminar() {
		HorarioEntity turno = view.getPanelHorario().getSeleccionado();
		if (seleccionado == null || turno == null)
			throw new ApplicationException("Hay que seleccionar un turno pulsando sobre él en el horario");
		if (esEspecifico() && !turno.isEspecifico())
			throw new ApplicationException(
					"Es un turno del horario periódico: para eliminarlo elige el tipo de horario periódico");
		int respuesta = JOptionPane.showConfirmDialog(view.getFrame(), "¿Seguro que quieres eliminar el turno?",
				"Eliminar turno", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (respuesta != JOptionPane.YES_OPTION)
			return;
		if (esEspecifico())
			model.eliminarTurnoEspecifico(turno.getId());
		else
			model.eliminarTurno(turno.getId());
		mostrarHorario();
	}

	/**
	 * Validaciones sintacticas de los datos del turno periodico y conversion a la entidad.
	 * Es estatico y no usa la vista para poder probarlo de forma independiente.
	 */
	public static HorarioEntity crearTurno(int empleadoId, String dia, String inicio, String fin) {
		DiaSemana d = dia == null ? null : DiaSemana.fromNombre(dia);
		if (d == null)
			throw new ApplicationException("Hay que seleccionar el día de la semana");
		HorarioEntity h = new HorarioEntity();
		h.setEmpleadoId(empleadoId);
		h.setDiaSemana(d.getNumero());
		h.setHoraInicio(parsearHora(inicio, "Hay que indicar la hora de inicio"));
		h.setHoraFin(parsearHora(fin, "Hay que indicar la hora de fin"));
		return h;
	}

	/**
	 * Validaciones sintacticas de los datos del turno de dia especifico y conversion a la entidad.
	 * Es estatico y no usa la vista para poder probarlo de forma independiente.
	 */
	public static HorarioEspecificoEntity crearTurnoEspecifico(int empleadoId, String fecha, String inicio, String fin) {
		if (fecha == null || fecha.trim().isEmpty())
			throw new ApplicationException("Hay que seleccionar el día");
		try {
			LocalDate.parse(fecha.trim());
		} catch (DateTimeParseException e) {
			throw new ApplicationException("La fecha debe tener el formato aaaa-mm-dd");
		}
		HorarioEspecificoEntity h = new HorarioEspecificoEntity();
		h.setEmpleadoId(empleadoId);
		h.setFecha(fecha.trim());
		h.setHoraInicio(parsearHora(inicio, "Hay que indicar la hora de inicio"));
		h.setHoraFin(parsearHora(fin, "Hay que indicar la hora de fin"));
		return h;
	}

	/** Convierte HH:mm (00:00 a 24:00) en minutos desde las 00:00 */
	private static int parsearHora(String texto, String mensajeVacio) {
		if (texto == null || texto.trim().isEmpty())
			throw new ApplicationException(mensajeVacio);
		Matcher m = HORA.matcher(texto.trim());
		String error = "La hora debe tener el formato HH:mm (entre 00:00 y 24:00)";
		if (!m.matches())
			throw new ApplicationException(error);
		int horas = Integer.parseInt(m.group(1));
		int minutos = Integer.parseInt(m.group(2));
		if (minutos > 59 || horas > 24 || (horas == 24 && minutos != 0))
			throw new ApplicationException(error);
		return horas * 60 + minutos;
	}

	/** Minutos desde las 00:00 (o duracion) en formato HH:mm */
	private static String formatearHora(int minutos) {
		return String.format("%02d:%02d", minutos / 60, minutos % 60);
	}
}
