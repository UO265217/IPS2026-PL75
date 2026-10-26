package giis.ips.empleados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador de los horarios de dia especifico de empleados no deportivos (HU 35204): el gerente elige
 * un empleado y un dia del calendario y anyade o elimina turnos de ese dia viendo su semana completa.
 * Realiza las validaciones sintacticas y llama al modelo.
 */
public class HorarioEspecificoController {
	private static final String[] COLUMNAS_EMPLEADOS = { "Id", "Apellido", "Nombre", "Posición" };
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private HorarioModel model;
	private HorarioEspecificoView view;
	private EmpleadoEntity seleccionado; // empleado cuyo horario se muestra o null si no hay ninguno

	public HorarioEspecificoController(HorarioModel m, HorarioEspecificoView v) {
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
		// Cualquier cambio de fecha (en el selector o con las flechas) vuelve a mostrar el dia y su semana
		view.getDcFecha().addPropertyChangeListener("date", e -> SwingUtil.exceptionWrapper(() -> mostrarHorario()));
		view.getBtnAnterior().addActionListener(e -> SwingUtil.exceptionWrapper(() -> moverDia(-1)));
		view.getBtnSiguiente().addActionListener(e -> SwingUtil.exceptionWrapper(() -> moverDia(1)));
		view.getBtnAnyadir().addActionListener(e -> SwingUtil.exceptionWrapper(() -> anyadir()));
		view.getBtnEliminar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> eliminar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
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
	 * Muestra la semana del dia seleccionado (resaltando ese dia) con el horario del empleado y las horas
	 * que lleva ese dia y esa semana; los botones solo se activan si hay un empleado seleccionado
	 */
	private void mostrarHorario() {
		LocalDate fecha = getFecha();
		LocalDate lunes = HorarioModel.getLunes(fecha);
		view.getLblSemana().setText("Semana del " + lunes.format(FECHA) + " al " + lunes.plusDays(6).format(FECHA)
				+ " (en naranja los turnos de día específico; resaltado el día " + fecha.format(FECHA) + ")");
		view.getPanelHorario().setLunes(lunes);
		view.getPanelHorario().setMarcado(fecha);
		boolean hay = seleccionado != null;
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
		view.getLblTotal().setText(hay ? "Día " + fecha.format(FECHA) + ": " + HorarioController.formatearHora(minutosDia)
				+ " de 08:00 horas · Semana: " + HorarioController.formatearHora(minutosSemana) + " de 40:00 horas" : " ");
		view.getBtnAnyadir().setEnabled(hay);
		view.getBtnEliminar().setEnabled(hay);
	}

	public void anyadir() {
		if (seleccionado == null)
			throw new ApplicationException("Hay que seleccionar un empleado");
		HorarioEspecificoEntity turno = crearTurno(seleccionado.getId(), SwingUtil.getIsoDate(view.getDcFecha()),
				leerHora(view.getSpHoraInicio(), view.getSpMinInicio()), leerHora(view.getSpHoraFin(), view.getSpMinFin()));
		model.anyadirTurnoEspecifico(turno);
		mostrarHorario();
	}

	/** Hora de los spinners en formato HH:mm, para validarla igual que el resto de horas */
	private static String leerHora(JSpinner spHora, JSpinner spMin) {
		return HorarioController.formatearHora((Integer) spHora.getValue() * 60 + (Integer) spMin.getValue());
	}

	public void eliminar() {
		HorarioEntity turno = view.getPanelHorario().getSeleccionado();
		if (seleccionado == null || turno == null)
			throw new ApplicationException("Hay que seleccionar un turno pulsando sobre él en el horario");
		if (!turno.isEspecifico())
			throw new ApplicationException(
					"Solo se pueden eliminar turnos de día específico; el periódico se cambia en Horarios de empleados");
		int respuesta = JOptionPane.showConfirmDialog(view.getFrame(), "¿Seguro que quieres eliminar el turno?",
				"Eliminar turno", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (respuesta != JOptionPane.YES_OPTION)
			return;
		model.eliminarTurnoEspecifico(turno.getId());
		mostrarHorario();
	}

	/**
	 * Validaciones sintacticas de los datos del turno de dia especifico y conversion a la entidad.
	 * Es estatico y no usa la vista para poder probarlo de forma independiente.
	 */
	public static HorarioEspecificoEntity crearTurno(int empleadoId, String fecha, String inicio, String fin) {
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
		h.setHoraInicio(HorarioController.parsearHora(inicio, "Hay que indicar la hora de inicio"));
		h.setHoraFin(HorarioController.parsearHora(fin, "Hay que indicar la hora de fin"));
		return h;
	}
}
