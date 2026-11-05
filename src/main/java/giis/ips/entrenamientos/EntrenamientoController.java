package giis.ips.entrenamientos;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

import giis.ips.reservas.InstalacionEntity;
import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador de la asignacion de entrenamientos a instalaciones (HU 36124): previsualiza el dia de la instalacion
 * con lo que se eliminara al asignar el entrenamiento, pide confirmacion si se elimina algo y lo guarda.
 */
public class EntrenamientoController {
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final Color ROJO = new Color(0xB7, 0x1C, 0x1C);
	private static final Color NARANJA = new Color(0xB2, 0x5E, 0x00);
	private static final Color VERDE = new Color(0x1B, 0x5E, 0x20);

	private EntrenamientoModel model;
	private EntrenamientoView view;
	private List<EquipoEntity> equipos;
	private List<InstalacionEntity> instalaciones;

	public EntrenamientoController(EntrenamientoModel m, EntrenamientoView v) {
		this.model = m;
		this.view = v;
		this.initView();
	}

	/** Inicializacion del controlador: anyade los manejadores de eventos */
	public void initController() {
		view.getCbEquipo().addActionListener(e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		view.getCbInstalacion().addActionListener(e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		view.getDcFecha().addPropertyChangeListener("date", e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		for (JSpinner sp : new JSpinner[] { view.getSpHoraInicio(), view.getSpMinInicio(), view.getSpHoraFin(),
				view.getSpMinFin() })
			sp.addChangeListener(e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		view.getBtnAsignar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> asignar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		equipos = model.getEquipos();
		DefaultComboBoxModel<String> ce = new DefaultComboBoxModel<>();
		for (EquipoEntity e : equipos)
			ce.addElement(e.getNombre());
		view.getCbEquipo().setModel(ce);

		instalaciones = model.getInstalaciones();
		DefaultComboBoxModel<String> ci = new DefaultComboBoxModel<>();
		for (InstalacionEntity i : instalaciones)
			ci.addElement(i.getNombre());
		view.getCbInstalacion().setModel(ci);

		SwingUtil.setIsoDate(view.getDcFecha(), LocalDate.now().toString());
		// Propuesta inicial: de 17:00 a 18:30
		setHora(view.getSpHoraInicio(), view.getSpMinInicio(), 17 * 60);
		setHora(view.getSpHoraFin(), view.getSpMinFin(), 18 * 60 + 30);
		previsualizar();
		view.getFrame().setVisible(true);
	}

	/** Fecha del selector o, si se ha borrado, la de hoy */
	private LocalDate getFecha() {
		String iso = SwingUtil.getIsoDate(view.getDcFecha());
		return iso.isEmpty() ? LocalDate.now() : LocalDate.parse(iso);
	}

	/** Equipo elegido en el desplegable o null si no hay ninguno */
	private EquipoEntity getEquipo() {
		int i = view.getCbEquipo().getSelectedIndex();
		return i < 0 ? null : equipos.get(i);
	}

	/** Instalacion elegida en el desplegable o null si no hay ninguna */
	private InstalacionEntity getInstalacion() {
		int i = view.getCbInstalacion().getSelectedIndex();
		return i < 0 ? null : instalaciones.get(i);
	}

	private static void setHora(JSpinner spHora, JSpinner spMin, int minutos) {
		spHora.setValue(minutos / 60);
		spMin.setValue(minutos % 60);
	}

	private static int leerHora(JSpinner spHora, JSpinner spMin) {
		return (Integer) spHora.getValue() * 60 + (Integer) spMin.getValue();
	}

	/**
	 * Repinta la previsualizacion del dia con el entrenamiento indicado y resume debajo si se puede asignar y que
	 * se eliminaria
	 */
	private void previsualizar() {
		InstalacionEntity inst = getInstalacion();
		if (inst == null) {
			view.getPanelPrevisualizacion().setDatos(null, null, null, 0, 0);
			mostrarEstado(" ", Color.BLACK);
			return;
		}
		int inicio = leerHora(view.getSpHoraInicio(), view.getSpMinInicio());
		int fin = leerHora(view.getSpHoraFin(), view.getSpMinFin());
		List<UsoInstalacionDTO> usos = model.getUsos(inst.getId(), getFecha());
		List<UsoInstalacionDTO> conflictos = EntrenamientoModel.getConflictos(usos, inicio, fin);
		// Si no se puede asignar, no se eliminara nada
		List<UsoInstalacionDTO> afectados = conflictos.isEmpty() ? EntrenamientoModel.getAfectados(usos, inicio, fin)
				: new ArrayList<>();
		view.getPanelPrevisualizacion().setDatos(usos, afectados, conflictos, inicio, fin);

		if (fin <= inicio)
			mostrarEstado("La hora de fin debe ser posterior a la de inicio", ROJO);
		else if (fin > EntrenamientoModel.FIN_DIA)
			mostrarEstado("La hora de fin no puede pasar de las 24:00", ROJO);
		else if (!conflictos.isEmpty())
			mostrarEstado("No se puede asignar: coincide con " + conflictos.get(0).describir(), ROJO);
		else if (!afectados.isEmpty())
			mostrarEstado("Al asignarlo se eliminará: " + resumirAfectados(afectados), NARANJA);
		else
			mostrarEstado("La instalación está libre en ese horario", VERDE);
	}

	private void mostrarEstado(String texto, Color color) {
		view.getLblEstado().setText(texto);
		view.getLblEstado().setForeground(color);
	}

	public void asignar() {
		EquipoEntity equipo = getEquipo();
		if (equipo == null)
			throw new ApplicationException("Hay que seleccionar un equipo");
		InstalacionEntity inst = getInstalacion();
		if (inst == null)
			throw new ApplicationException("Hay que seleccionar una instalación");
		EntrenamientoEntity e = new EntrenamientoEntity();
		e.setEquipoId(equipo.getId());
		e.setInstalacionId(inst.getId());
		e.setFecha(getFecha().toString());
		e.setHoraInicio(leerHora(view.getSpHoraInicio(), view.getSpMinInicio()));
		e.setHoraFin(leerHora(view.getSpHoraFin(), view.getSpMinFin()));

		model.validar(e);
		String descripcion = "el entrenamiento del " + equipo.getNombre() + " en " + inst.getNombre() + " el "
				+ getFecha().format(FECHA) + " de " + EntrenamientoModel.formatearHora(e.getHoraInicio()) + " a "
				+ EntrenamientoModel.formatearHora(e.getHoraFin());

		// Si se va a eliminar algo, se detalla y se pide confirmacion
		List<UsoInstalacionDTO> afectados = model.getAfectados(e);
		if (!afectados.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(mayuscula(descripcion)).append(" eliminará:\n");
			for (UsoInstalacionDTO u : afectados)
				sb.append("  · ").append(u.describir()).append("\n");
			sb.append("\n¿Quieres asignarlo igualmente?");
			Object[] opciones = { "Sí, asignar", "No" };
			int r = JOptionPane.showOptionDialog(view.getFrame(), sb.toString(), "Confirmar asignación",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, opciones, opciones[1]);
			if (r != JOptionPane.YES_OPTION)
				return;
		}

		List<UsoInstalacionDTO> eliminados = model.asignar(e);
		JOptionPane.showMessageDialog(view.getFrame(),
				"Entrenamiento asignado: " + descripcion + "."
						+ (eliminados.isEmpty() ? "" : (eliminados.size() == 1 ? "\nSe ha eliminado " : "\nSe han eliminado ") + resumirAfectados(eliminados) + "."),
				"Entrenamiento asignado", JOptionPane.INFORMATION_MESSAGE);
		previsualizar();
	}

	/** Resumen del tipo "1 reserva externa y 2 labores de jardinería" */
	private static String resumirAfectados(List<UsoInstalacionDTO> afectados) {
		int reservas = 0;
		for (UsoInstalacionDTO u : afectados)
			if (UsoInstalacionDTO.RESERVA.equals(u.getTipo()))
				reservas++;
		int labores = afectados.size() - reservas;
		String txtReservas = reservas == 1 ? "1 reserva externa" : reservas + " reservas externas";
		String txtLabores = labores == 1 ? "1 labor de jardinería" : labores + " labores de jardinería";
		if (labores == 0)
			return txtReservas;
		return reservas == 0 ? txtLabores : txtReservas + " y " + txtLabores;
	}

	private static String mayuscula(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
