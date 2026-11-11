package giis.ips.jardineria;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

import giis.ips.entrenamientos.EntrenamientoModel;
import giis.ips.entrenamientos.UsoInstalacionDTO;
import giis.ips.reservas.FranjaDTO;
import giis.ips.reservas.InstalacionEntity;
import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador de la programacion de labores de jardineria (HU 36125): previsualiza el dia de la instalacion y del
 * jardinero con la labor que se esta programando, indica si se puede y por que no, y la guarda.
 */
public class JardineriaController {
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final Color ROJO = new Color(0xB7, 0x1C, 0x1C);
	private static final Color VERDE = new Color(0x1B, 0x5E, 0x20);
	private static final Color GRIS = new Color(0x55, 0x55, 0x55);

	private JardineriaModel model;
	private JardineriaView view;
	private List<InstalacionEntity> instalaciones;
	private List<JardineroDTO> jardineros;

	public JardineriaController(JardineriaModel m, JardineriaView v) {
		this.model = m;
		this.view = v;
		this.initView();
	}

	/** Inicializacion del controlador: anyade los manejadores de eventos */
	public void initController() {
		view.getCbInstalacion().addActionListener(e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		view.getCbJardinero().addActionListener(e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		view.getDcFecha().addPropertyChangeListener("date", e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		for (JSpinner sp : new JSpinner[] { view.getSpHoraInicio(), view.getSpMinInicio(), view.getSpHoraFin(),
				view.getSpMinFin() })
			sp.addChangeListener(e -> SwingUtil.exceptionWrapper(() -> previsualizar()));
		view.getBtnProgramar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> programar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		instalaciones = model.getInstalaciones();
		DefaultComboBoxModel<String> ci = new DefaultComboBoxModel<>();
		for (InstalacionEntity i : instalaciones)
			ci.addElement(i.getNombre());
		view.getCbInstalacion().setModel(ci);

		jardineros = model.getJardineros();
		DefaultComboBoxModel<String> cj = new DefaultComboBoxModel<>();
		for (JardineroDTO j : jardineros)
			cj.addElement(j.getNombreCompleto());
		view.getCbJardinero().setModel(cj);

		SwingUtil.setIsoDate(view.getDcFecha(), LocalDate.now().toString());
		// Propuesta inicial: de 9:00 a 10:00
		setHora(view.getSpHoraInicio(), view.getSpMinInicio(), 9 * 60);
		setHora(view.getSpHoraFin(), view.getSpMinFin(), 10 * 60);
		previsualizar();
		view.getFrame().setVisible(true);
	}

	/** Fecha del selector o, si se ha borrado, la de hoy */
	private LocalDate getFecha() {
		String iso = SwingUtil.getIsoDate(view.getDcFecha());
		return iso.isEmpty() ? LocalDate.now() : LocalDate.parse(iso);
	}

	/** Instalacion elegida en el desplegable o null si no hay ninguna */
	private InstalacionEntity getInstalacion() {
		int i = view.getCbInstalacion().getSelectedIndex();
		return i < 0 ? null : instalaciones.get(i);
	}

	/** Jardinero elegido en el desplegable o null si no hay ninguno */
	private JardineroDTO getJardinero() {
		int i = view.getCbJardinero().getSelectedIndex();
		return i < 0 ? null : jardineros.get(i);
	}

	private static void setHora(JSpinner spHora, JSpinner spMin, int minutos) {
		spHora.setValue(minutos / 60);
		spMin.setValue(minutos % 60);
	}

	private static int leerHora(JSpinner spHora, JSpinner spMin) {
		return (Integer) spHora.getValue() * 60 + (Integer) spMin.getValue();
	}

	/**
	 * Repinta la previsualizacion con la labor indicada, muestra los turnos del jardinero ese dia y resume debajo si
	 * se puede programar o por que no
	 */
	private void previsualizar() {
		InstalacionEntity inst = getInstalacion();
		JardineroDTO jard = getJardinero();
		if (inst == null || jard == null) {
			view.getPanelPrevisualizacion().setDatos(null, null, null, 0, 0, 0, false);
			view.getLblTurnos().setText(" ");
			mostrarEstado(inst == null ? "Hay que seleccionar una instalación" : "Hay que seleccionar un jardinero",
					ROJO);
			return;
		}
		LocalDate fecha = getFecha();
		int inicio = leerHora(view.getSpHoraInicio(), view.getSpMinInicio());
		int fin = leerHora(view.getSpHoraFin(), view.getSpMinFin());
		List<UsoInstalacionDTO> usos = model.getUsosInstalacion(inst.getId(), fecha, jard.getId());
		List<FranjaDTO> turnos = model.getTurnos(jard.getId(), fecha);
		List<LaborJardineroDTO> labores = model.getLaboresJardinero(jard.getId(), fecha);

		if (turnos.isEmpty()) {
			view.getLblTurnos().setText("No trabaja este día");
			view.getLblTurnos().setForeground(ROJO);
		} else {
			view.getLblTurnos().setText("Turnos este día: " + JardineriaModel.describirTurnos(turnos));
			view.getLblTurnos().setForeground(GRIS);
		}

		String motivo = fecha.isBefore(LocalDate.now()) ? "No se puede programar una labor en una fecha pasada"
				: JardineriaModel.motivoNoProgramable(inicio, fin, jard.getNombreCompleto(), turnos, labores, usos);
		view.getPanelPrevisualizacion().setDatos(usos, turnos, labores, inst.getId(), inicio, fin, motivo == null);
		if (motivo == null)
			mostrarEstado("Se puede programar: la instalación está libre y " + jard.getNombreCompleto()
					+ " está de turno de " + EntrenamientoModel.formatearHora(inicio) + " a "
					+ EntrenamientoModel.formatearHora(fin), VERDE);
		else
			mostrarEstado("No se puede programar: " + minuscula(motivo), ROJO);
	}

	private void mostrarEstado(String texto, Color color) {
		view.getLblEstado().setText(texto);
		view.getLblEstado().setForeground(color);
	}

	public void programar() {
		InstalacionEntity inst = getInstalacion();
		if (inst == null)
			throw new ApplicationException("Hay que seleccionar una instalación");
		JardineroDTO jard = getJardinero();
		if (jard == null)
			throw new ApplicationException("Hay que seleccionar un jardinero");
		LaborJardineriaEntity l = new LaborJardineriaEntity();
		l.setInstalacionId(inst.getId());
		l.setEmpleadoId(jard.getId());
		l.setDescripcion(view.getTxtDescripcion().getText());
		l.setFecha(getFecha().toString());
		l.setHoraInicio(leerHora(view.getSpHoraInicio(), view.getSpMinInicio()));
		l.setHoraFin(leerHora(view.getSpHoraFin(), view.getSpMinFin()));
		model.programar(l);

		JOptionPane.showMessageDialog(view.getFrame(),
				"Labor de jardinería programada.\n" + l.getDescripcion().trim() + " · " + inst.getNombre() + " · "
						+ getFecha().format(FECHA) + " · " + EntrenamientoModel.formatearHora(l.getHoraInicio())
						+ " - " + EntrenamientoModel.formatearHora(l.getHoraFin()) + "\nJardinero: "
						+ jard.getNombreCompleto(),
				"Labor programada", JOptionPane.INFORMATION_MESSAGE);
		view.getTxtDescripcion().setText("");
		previsualizar();
	}

	/** Pone en minuscula la primera letra, salvo que empiece por un nombre propio (el del jardinero) */
	private String minuscula(String s) {
		JardineroDTO jard = getJardinero();
		if (jard != null && s.startsWith(jard.getNombre()))
			return s;
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}
}
