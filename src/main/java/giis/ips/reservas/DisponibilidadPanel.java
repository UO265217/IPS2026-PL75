package giis.ips.reservas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Dia de 8:00 a 22:00 de una instalacion, con dos pistas:
 * - "Actual": en verde las franjas libres, en rojo los usos de los equipos, en amarillo la hora y media posterior a
 *   cada uso, en azul las reservas y en blanco rayado de gris los huecos de menos de 1 hora. Lo que impide la nueva
 *   reserva lleva borde negro grueso.
 * - "Nueva": la reserva que se esta haciendo con las horas elegidas (verde si se puede, gris si no), que tambien se
 *   recuadra en la pista "Actual" para verla sobre la disponibilidad.
 * Pulsando sobre una franja libre de la pista "Actual" se avisa al oyente indicado con setOnFranja.
 */
public class DisponibilidadPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final int ALTO_PISTA = 30;
	private static final int SEPARACION = 6;
	private static final int ALTO_HORAS = 16;
	private static final int ANCHO_ETIQUETA = 60;
	private static final int MARGEN_DER = 20;

	private static final Color LIBRE = new Color(0x9B, 0xD3, 0x9B);
	private static final Color EQUIPO = new Color(0xE5, 0x73, 0x73);
	private static final Color MARGEN = new Color(0xFF, 0xD5, 0x4F);
	private static final Color RESERVA = new Color(0x7F, 0xA7, 0xDD);
	private static final Color CORTO = Color.WHITE;
	private static final Color CORTO_RAYA = new Color(0xB0, 0xB0, 0xB0);
	private static final Color NUEVA = new Color(0x2E, 0x7D, 0x32);
	private static final Color NUEVA_IMPOSIBLE = new Color(0x9E, 0x9E, 0x9E);
	private static final Color SELECCION = new Color(0x1E, 0x64, 0xC8);

	private List<FranjaDTO> usos = new ArrayList<>();
	private List<ReservaEntity> reservas = new ArrayList<>();
	private List<FranjaDTO> libres = new ArrayList<>();
	private int nuevaInicio;
	private int nuevaFin;
	private boolean nuevaPosible;
	private transient Consumer<FranjaDTO> onFranja = f -> { };

	public DisponibilidadPanel() {
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(700, 2 * ALTO_PISTA + SEPARACION + ALTO_HORAS + 4));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getY() > ALTO_PISTA)
					return;
				int minuto = aMinuto(e.getX());
				for (FranjaDTO f : libres)
					if (minuto >= f.getHoraInicio() && minuto < f.getHoraFin())
						onFranja.accept(f);
			}
		});
	}

	public void setOnFranja(Consumer<FranjaDTO> onFranja) {
		this.onFranja = onFranja;
	}

	/** Datos del dia mostrado; cualquiera de las listas puede ser null si no hay instalacion seleccionada */
	public void setDia(List<FranjaDTO> usos, List<ReservaEntity> reservas, List<FranjaDTO> libres) {
		this.usos = usos == null ? new ArrayList<>() : usos;
		this.reservas = reservas == null ? new ArrayList<>() : reservas;
		this.libres = libres == null ? new ArrayList<>() : libres;
		repaint();
	}

	/**
	 * Reserva que se esta haciendo, en minutos desde las 00:00, y si se puede hacer. Si el fin no es posterior al
	 * inicio no se dibuja
	 */
	public void setNueva(int inicio, int fin, boolean posible) {
		this.nuevaInicio = inicio;
		this.nuevaFin = fin;
		this.nuevaPosible = posible;
		repaint();
	}

	private int ancho() {
		return getWidth() - ANCHO_ETIQUETA - MARGEN_DER;
	}

	private int aX(int minuto) {
		int m = Math.max(ReservaModel.APERTURA, Math.min(ReservaModel.CIERRE, minuto));
		return ANCHO_ETIQUETA + (m - ReservaModel.APERTURA) * ancho() / (ReservaModel.CIERRE - ReservaModel.APERTURA);
	}

	private int aMinuto(int x) {
		return ReservaModel.APERTURA + (x - ANCHO_ETIQUETA) * (ReservaModel.CIERRE - ReservaModel.APERTURA) / ancho();
	}

	private boolean hayNueva() {
		return nuevaFin > nuevaInicio && aX(nuevaFin) > aX(nuevaInicio);
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
		FontMetrics fm = g.getFontMetrics();
		int yNueva = ALTO_PISTA + SEPARACION;

		// Pista "Actual": lo que no es libre ni esta ocupado son huecos de menos de 1 hora
		int x0 = aX(ReservaModel.APERTURA);
		rayar(g, x0, 0, aX(ReservaModel.CIERRE) - x0, ALTO_PISTA);
		for (FranjaDTO f : libres)
			pintar(g, 0, f.getHoraInicio(), f.getHoraFin(), LIBRE, "libre", Color.BLACK);
		for (FranjaDTO u : usos) {
			pintar(g, 0, u.getHoraFin(), u.getHoraFin() + ReservaModel.MARGEN_TRAS_EQUIPO, MARGEN, "", Color.BLACK);
			pintar(g, 0, u.getHoraInicio(), u.getHoraFin(), EQUIPO, "equipo", Color.BLACK);
		}
		for (ReservaEntity r : reservas)
			pintar(g, 0, r.getHoraInicio(), r.getHoraFin(), RESERVA, "reservada", Color.BLACK);

		if (hayNueva()) {
			// Lo que impide la reserva, con borde negro grueso
			for (FranjaDTO u : usos) {
				int finMargen = u.getHoraFin() + ReservaModel.MARGEN_TRAS_EQUIPO;
				if (ReservaModel.seSolapan(nuevaInicio, nuevaFin, u.getHoraInicio(), finMargen))
					borde(g, 0, u.getHoraInicio(), finMargen, Color.BLACK, 3);
			}
			for (ReservaEntity r : reservas)
				if (ReservaModel.seSolapan(nuevaInicio, nuevaFin, r.getHoraInicio(), r.getHoraFin()))
					borde(g, 0, r.getHoraInicio(), r.getHoraFin(), Color.BLACK, 3);
			// La seccion que se reserva, recuadrada sobre la disponibilidad
			borde(g, 0, nuevaInicio, nuevaFin, SELECCION, 2);

			// Pista "Nueva"
			String txt = nuevaPosible ? hora(nuevaInicio) + " - " + hora(nuevaFin) : "No se puede";
			if (fm.stringWidth(txt) + 6 >= aX(nuevaFin) - aX(nuevaInicio))
				txt = nuevaPosible ? hora(nuevaInicio) + "-" + hora(nuevaFin) : "No";
			pintar(g, yNueva, nuevaInicio, nuevaFin, nuevaPosible ? NUEVA : NUEVA_IMPOSIBLE, txt, Color.WHITE);
		}

		// Marcos y etiquetas de las pistas
		g.setColor(Color.GRAY);
		g.drawRect(ANCHO_ETIQUETA, 0, ancho(), ALTO_PISTA);
		g.drawRect(ANCHO_ETIQUETA, yNueva, ancho(), ALTO_PISTA);
		g.setColor(Color.DARK_GRAY);
		g.drawString("Actual", ANCHO_ETIQUETA - 6 - fm.stringWidth("Actual"), (ALTO_PISTA + fm.getAscent()) / 2 - 1);
		g.drawString("Nueva", ANCHO_ETIQUETA - 6 - fm.stringWidth("Nueva"),
				yNueva + (ALTO_PISTA + fm.getAscent()) / 2 - 1);

		// Escala de horas cada 2 horas
		int yHoras = yNueva + ALTO_PISTA;
		for (int h = ReservaModel.APERTURA / 60; h <= ReservaModel.CIERRE / 60; h++) {
			int x = aX(h * 60);
			g.setColor(Color.GRAY);
			g.drawLine(x, yHoras, x, yHoras + 3);
			if (h % 2 == 0) {
				String t = h + ":00";
				g.setColor(Color.DARK_GRAY);
				g.drawString(t, x - fm.stringWidth(t) / 2, yHoras + ALTO_HORAS);
			}
		}
	}

	private static String hora(int minutos) {
		return String.format("%d:%02d", minutos / 60, minutos % 60);
	}

	private void pintar(Graphics2D g, int y, int inicio, int fin, Color color, String texto, Color colorTexto) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		if (x2 <= x1)
			return;
		g.setColor(color);
		g.fillRect(x1, y, x2 - x1, ALTO_PISTA);
		g.setColor(Color.WHITE);
		g.drawLine(x2 - 1, y, x2 - 1, y + ALTO_PISTA);
		FontMetrics fm = g.getFontMetrics();
		if (!texto.isEmpty() && fm.stringWidth(texto) + 6 < x2 - x1) {
			g.setColor(colorTexto);
			g.drawString(texto, x1 + (x2 - x1 - fm.stringWidth(texto)) / 2, y + (ALTO_PISTA + fm.getAscent()) / 2 - 1);
		}
	}

	private void borde(Graphics2D g, int y, int inicio, int fin, Color color, int grosor) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		if (x2 - x1 <= grosor)
			return;
		g.setColor(color);
		g.setStroke(new BasicStroke(grosor));
		g.drawRect(x1 + grosor / 2, y + grosor / 2, x2 - x1 - grosor, ALTO_PISTA - grosor);
		g.setStroke(new BasicStroke(1));
	}

	/** Rectangulo blanco con rayas grises en diagonal, el de los huecos de menos de 1 hora */
	private static void rayar(Graphics g, int x, int y, int ancho, int alto) {
		if (ancho <= 0)
			return;
		g.setColor(CORTO);
		g.fillRect(x, y, ancho, alto);
		Graphics g2 = g.create(x, y, ancho, alto);
		g2.setColor(CORTO_RAYA);
		for (int i = -alto; i < ancho; i += 8)
			g2.fillPolygon(new int[] { i, i + 3, i + 3 + alto, i + alto }, new int[] { alto, alto, 0, 0 }, 4);
		g2.dispose();
	}

	/** Leyenda de las pistas, con muestras pintadas igual que en ellas */
	public static JPanel crearLeyenda() {
		JPanel leyenda = new JPanel(new MigLayout("insets 0, wrap 5", "", ""));
		leyenda.add(muestra("Libre", LIBRE, false, null));
		leyenda.add(muestra("Uso de un equipo", EQUIPO, false, null), "gapleft 10");
		leyenda.add(muestra("1 h 30 min tras el equipo", MARGEN, false, null), "gapleft 10");
		leyenda.add(muestra("Otra reserva", RESERVA, false, null), "gapleft 10");
		leyenda.add(muestra("Hueco de menos de 1 h", CORTO, true, null), "gapleft 10");
		leyenda.add(muestra("Nueva reserva", NUEVA, false, null));
		leyenda.add(muestra("No se puede reservar", NUEVA_IMPOSIBLE, false, null), "gapleft 10");
		leyenda.add(muestra("Sección elegida", Color.WHITE, false, SELECCION), "gapleft 10");
		leyenda.add(muestra("Impide reservar", EQUIPO, false, Color.BLACK), "gapleft 10");
		return leyenda;
	}

	private static JLabel muestra(String texto, Color color, boolean rayado, Color borde) {
		JLabel lbl = new JLabel(texto, new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				if (rayado)
					rayar(g, x, y, 14, 14);
				else {
					g.setColor(color);
					g.fillRect(x, y, 14, 14);
				}
				g.setColor(borde == null ? Color.GRAY : borde);
				g.drawRect(x, y, 13, 13);
				if (borde != null)
					g.drawRect(x + 1, y + 1, 11, 11);
			}

			@Override
			public int getIconWidth() {
				return 14;
			}

			@Override
			public int getIconHeight() {
				return 14;
			}
		}, JLabel.LEFT);
		lbl.setFont(lbl.getFont().deriveFont(11f));
		return lbl;
	}
}
