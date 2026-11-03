package giis.ips.reservas;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador de las reservas de instalaciones por usuarios externos (HU 35206): muestra la disponibilidad
 * de la instalacion en el dia elegido, calcula el importe segun se cambian las horas y guarda la reserva.
 */
public class ReservaController {
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private ReservaModel model;
	private ReservaView view;
	private List<InstalacionEntity> instalaciones;

	public ReservaController(ReservaModel m, ReservaView v) {
		this.model = m;
		this.view = v;
		this.initView();
	}

	/** Inicializacion del controlador: anyade los manejadores de eventos */
	public void initController() {
		view.getCbInstalacion().addActionListener(e -> SwingUtil.exceptionWrapper(() -> mostrarDia()));
		view.getDcFecha().addPropertyChangeListener("date", e -> SwingUtil.exceptionWrapper(() -> mostrarDia()));
		view.getPanelDisponibilidad().setOnFranja(f -> SwingUtil.exceptionWrapper(() -> elegirFranja(f)));
		for (JSpinner sp : new JSpinner[] { view.getSpHoraInicio(), view.getSpMinInicio(), view.getSpHoraFin(),
				view.getSpMinFin() })
			sp.addChangeListener(e -> mostrarImporte());
		view.getBtnReservar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> reservar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		instalaciones = model.getInstalaciones();
		DefaultComboBoxModel<String> cm = new DefaultComboBoxModel<>();
		for (InstalacionEntity i : instalaciones)
			cm.addElement(i.getNombre());
		view.getCbInstalacion().setModel(cm);
		SwingUtil.setIsoDate(view.getDcFecha(), LocalDate.now().toString());
		mostrarDia();
		mostrarImporte();
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

	/** Muestra en la barra la disponibilidad de la instalacion en la fecha elegida */
	private void mostrarDia() {
		InstalacionEntity inst = getInstalacion();
		if (inst == null) {
			view.getPanelDisponibilidad().setDia(null, null, null);
			return;
		}
		LocalDate fecha = getFecha();
		view.getPanelDisponibilidad().setDia(model.getUsosEquipos(inst.getId(), fecha),
				model.getReservas(inst.getId(), fecha), model.getFranjasLibres(inst.getId(), fecha));
	}

	/** Propone una reserva de la duracion minima (1 hora) desde el inicio de la franja elegida */
	private void elegirFranja(FranjaDTO f) {
		view.getPanelDisponibilidad().setSeleccionada(f);
		setHora(view.getSpHoraInicio(), view.getSpMinInicio(), f.getHoraInicio());
		setHora(view.getSpHoraFin(), view.getSpMinFin(), f.getHoraInicio() + ReservaModel.DURACION_MINIMA);
	}

	private static void setHora(JSpinner spHora, JSpinner spMin, int minutos) {
		spHora.setValue(minutos / 60);
		spMin.setValue(minutos % 60);
	}

	private static int leerHora(JSpinner spHora, JSpinner spMin) {
		return (Integer) spHora.getValue() * 60 + (Integer) spMin.getValue();
	}

	/** Importe que se cobraria con las horas indicadas (50 euros la hora, en proporcion al tiempo) */
	private void mostrarImporte() {
		int inicio = leerHora(view.getSpHoraInicio(), view.getSpMinInicio());
		int fin = leerHora(view.getSpHoraFin(), view.getSpMinFin());
		if (fin <= inicio) {
			view.getLblImporte().setText("-");
			return;
		}
		view.getLblImporte().setText(String.format("%.2f € (%s a %d € la hora)",
				ReservaModel.calcularImporte(inicio, fin), formatearDuracion(fin - inicio), ReservaModel.PRECIO_HORA));
	}

	public void reservar() {
		InstalacionEntity inst = getInstalacion();
		if (inst == null)
			throw new ApplicationException("Hay que seleccionar una instalación");
		ReservaEntity r = new ReservaEntity();
		r.setInstalacionId(inst.getId());
		r.setFecha(getFecha().toString());
		r.setHoraInicio(leerHora(view.getSpHoraInicio(), view.getSpMinInicio()));
		r.setHoraFin(leerHora(view.getSpHoraFin(), view.getSpMinFin()));
		r.setNombreUsuario(view.getTxtNombre().getText());
		r.setNumeroTarjeta(view.getTxtTarjeta().getText());
		model.reservar(r);

		String tarjeta = r.getNumeroTarjeta().trim();
		JOptionPane.showMessageDialog(view.getFrame(),
				"Reserva guardada: " + inst.getNombre() + ", " + getFecha().format(FECHA) + " de "
						+ formatearHora(r.getHoraInicio()) + " a " + formatearHora(r.getHoraFin()) + "\nUsuario: "
						+ r.getNombreUsuario().trim() + "\nCobrados " + String.format("%.2f €", r.getImporte())
						+ " en la tarjeta acabada en " + tarjeta.substring(Math.max(0, tarjeta.length() - 4)),
				"Reserva confirmada", JOptionPane.INFORMATION_MESSAGE);
		view.getTxtNombre().setText("");
		view.getTxtTarjeta().setText("");
		mostrarDia();
	}

	/** Minutos desde las 00:00 en formato HH:mm */
	private static String formatearHora(int minutos) {
		return String.format("%02d:%02d", minutos / 60, minutos % 60);
	}

	/** Duracion en formato "2 h 30 min" */
	private static String formatearDuracion(int minutos) {
		return minutos / 60 + " h" + (minutos % 60 == 0 ? "" : " " + minutos % 60 + " min");
	}
}
