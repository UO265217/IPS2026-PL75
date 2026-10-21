package giis.ips.empleados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import giis.ips.util.SwingUtil;

/**
 * Controlador de la visualizacion del horario de un empleado no deportivo en una semana concreta del calendario:
 * el empleado selecciona su nombre y la semana y se muestran sus turnos periodicos y de dias especificos.
 */
public class HorarioSemanaController {
	private static final String[] COLUMNAS_EMPLEADOS = { "Id", "Apellido", "Nombre", "Posición" };
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private HorarioModel model;
	private HorarioSemanaView view;
	private EmpleadoEntity seleccionado; // empleado cuyo horario se muestra o null si no hay ninguno

	public HorarioSemanaController(HorarioModel m, HorarioSemanaView v) {
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
		// Cualquier cambio de fecha (en el selector o con las flechas) vuelve a mostrar la semana
		view.getDcSemana().addPropertyChangeListener("date", e -> SwingUtil.exceptionWrapper(() -> mostrarHorario()));
		view.getBtnAnterior().addActionListener(e -> SwingUtil.exceptionWrapper(() -> moverSemana(-1)));
		view.getBtnSiguiente().addActionListener(e -> SwingUtil.exceptionWrapper(() -> moverSemana(1)));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		SwingUtil.setIsoDate(view.getDcSemana(), LocalDate.now().toString());
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

	/** Mueve la fecha seleccionada el numero de semanas indicado (si no hay ninguna, parte de hoy) */
	public void moverSemana(int semanas) {
		LocalDate fecha = getFecha();
		SwingUtil.setIsoDate(view.getDcSemana(), fecha.plusWeeks(semanas).toString());
	}

	/** Fecha del selector o, si se ha borrado, la de hoy */
	private LocalDate getFecha() {
		String iso = SwingUtil.getIsoDate(view.getDcSemana());
		return iso.isEmpty() ? LocalDate.now() : LocalDate.parse(iso);
	}

	/** Muestra la semana de la fecha seleccionada y el horario del empleado (o lo vacia si no hay ninguno) */
	private void mostrarHorario() {
		LocalDate lunes = HorarioModel.getLunes(getFecha());
		view.getLblSemana().setText("Semana del " + lunes.format(FECHA) + " al " + lunes.plusDays(6).format(FECHA)
				+ " (en naranja los días con horario específico)");
		view.getPanelHorario().setLunes(lunes);
		boolean hay = seleccionado != null;
		List<HorarioEntity> turnos = hay ? model.getHorarioSemana(seleccionado.getId(), lunes) : null;
		int totalMinutos = 0;
		if (hay)
			for (HorarioEntity h : turnos)
				totalMinutos += h.getHoraFin() - h.getHoraInicio();
		view.getPanelHorario().setTurnos(turnos);
		view.getLblTotal().setText(hay ? "Total de la semana: " + formatearHora(totalMinutos) + " horas" : " ");
	}

	/** Duracion en minutos en formato HH:mm */
	private static String formatearHora(int minutos) {
		return String.format("%02d:%02d", minutos / 60, minutos % 60);
	}
}
