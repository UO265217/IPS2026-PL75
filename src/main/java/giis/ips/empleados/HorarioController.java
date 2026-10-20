package giis.ips.empleados;

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
 * Controlador de los horarios periodicos de empleados no deportivos: instala los manejadores de la vista,
 * realiza las validaciones sintacticas y llama al modelo.
 */
public class HorarioController {
	private static final String[] COLUMNAS_EMPLEADOS = { "Id", "Apellido", "Nombre", "Posición" };
	private static final Pattern HORA = Pattern.compile("(\\d{2}):(\\d{2})");

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
		view.getBtnAnyadir().addActionListener(e -> SwingUtil.exceptionWrapper(() -> anyadir()));
		view.getBtnEliminar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> eliminar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		view.getCbDia().setModel(new DefaultComboBoxModel<>(DiaSemana.nombres().toArray(new String[0])));
		cargarEmpleados();
		mostrarHorario(null);
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
		if (key.isEmpty())
			mostrarHorario(null);
		else
			mostrarHorario(model.getEmpleadosNoDeportivos().stream()
					.filter(e -> String.valueOf(e.getId()).equals(key)).findFirst().orElse(null));
	}

	/** Muestra el horario del empleado (o lo vacia si es null) y activa los botones solo si hay uno */
	private void mostrarHorario(EmpleadoEntity e) {
		seleccionado = e;
		boolean hay = e != null;
		List<HorarioEntity> turnos = hay ? model.getHorarios(e.getId()) : null;
		int totalMinutos = 0;
		if (hay)
			for (HorarioEntity h : turnos)
				totalMinutos += h.getHoraFin() - h.getHoraInicio();
		view.getPanelHorario().setTurnos(turnos);
		view.getLblTotal().setText(hay ? "Total semanal: " + formatearHora(totalMinutos) + " de 40:00 horas" : " ");
		view.getBtnAnyadir().setEnabled(hay);
		view.getBtnEliminar().setEnabled(hay);
	}

	public void anyadir() {
		if (seleccionado == null)
			throw new ApplicationException("Hay que seleccionar un empleado");
		HorarioEntity turno = crearTurno(seleccionado.getId(), (String) view.getCbDia().getSelectedItem(),
				leerHora(view.getSpHoraInicio(), view.getSpMinInicio()), leerHora(view.getSpHoraFin(), view.getSpMinFin()));
		model.anyadirTurno(turno);
		mostrarHorario(seleccionado);
	}

	/** Hora de los spinners en formato HH:mm, para validarla igual que el resto de horas */
	private static String leerHora(JSpinner spHora, JSpinner spMin) {
		return formatearHora((Integer) spHora.getValue() * 60 + (Integer) spMin.getValue());
	}

	public void eliminar() {
		HorarioEntity turno = view.getPanelHorario().getSeleccionado();
		if (seleccionado == null || turno == null)
			throw new ApplicationException("Hay que seleccionar un turno pulsando sobre él en el horario");
		int respuesta = JOptionPane.showConfirmDialog(view.getFrame(), "¿Seguro que quieres eliminar el turno?",
				"Eliminar turno", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (respuesta != JOptionPane.YES_OPTION)
			return;
		model.eliminarTurno(turno.getId());
		mostrarHorario(seleccionado);
	}

	/**
	 * Validaciones sintacticas de los datos del turno y conversion a la entidad.
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
