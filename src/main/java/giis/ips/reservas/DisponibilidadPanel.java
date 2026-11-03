package giis.ips.reservas;

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
 * Barra con el dia de 8:00 a 22:00 de una instalacion: en verde las franjas libres, en rojo los usos de los equipos,
 * en amarillo la hora y media posterior a cada uso, en azul las reservas y en blanco rayado de gris los huecos de
 * menos de 1 hora.
 * Pulsando sobre una franja libre se avisa al oyente indicado con setOnFranja.
 */
public class DisponibilidadPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final int ALTO_BARRA = 34;
	private static final int ALTO_HORAS = 16;
	private static final int MARGEN_X = 20;

	private static final Color LIBRE = new Color(0x9B, 0xD3, 0x9B);
	private static final Color EQUIPO = new Color(0xE5, 0x73, 0x73);
	private static final Color MARGEN = new Color(0xFF, 0xD5, 0x4F);
	private static final Color RESERVA = new Color(0x7F, 0xA7, 0xDD);
	private static final Color CORTO = Color.WHITE;
	private static final Color CORTO_RAYA = new Color(0xB0, 0xB0, 0xB0);
	private static final Color SELECCION = new Color(0x1E, 0x64, 0xC8);

	private List<FranjaDTO> usos = new ArrayList<>();
	private List<ReservaEntity> reservas = new ArrayList<>();
	private List<FranjaDTO> libres = new ArrayList<>();
	private FranjaDTO seleccionada;
	private transient Consumer<FranjaDTO> onFranja = f -> { };

	public DisponibilidadPanel() {
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(700, ALTO_BARRA + ALTO_HORAS + 4));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int minuto = aMinuto(e.getX());
				for (FranjaDTO f : libres)
					if (minuto >= f.getHoraInicio() && minuto < f.getHoraFin() && e.getY() <= ALTO_BARRA)
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
		this.seleccionada = null;
		repaint();
	}

	public void setSeleccionada(FranjaDTO seleccionada) {
		this.seleccionada = seleccionada;
		repaint();
	}

	private int ancho() {
		return getWidth() - 2 * MARGEN_X;
	}

	private int aX(int minuto) {
		int m = Math.max(ReservaModel.APERTURA, Math.min(ReservaModel.CIERRE, minuto));
		return MARGEN_X + (m - ReservaModel.APERTURA) * ancho() / (ReservaModel.CIERRE - ReservaModel.APERTURA);
	}

	private int aMinuto(int x) {
		return ReservaModel.APERTURA + (x - MARGEN_X) * (ReservaModel.CIERRE - ReservaModel.APERTURA) / ancho();
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(getFont().deriveFont(Font.PLAIN, 11f));

		// Lo que no es libre ni esta ocupado son huecos de menos de 1 hora
		pintarRayado(g, ReservaModel.APERTURA, ReservaModel.CIERRE);
		for (FranjaDTO f : libres)
			pintar(g, f.getHoraInicio(), f.getHoraFin(), LIBRE, "libre");
		for (FranjaDTO u : usos) {
			pintar(g, u.getHoraFin(), u.getHoraFin() + ReservaModel.MARGEN_TRAS_EQUIPO, MARGEN, "");
			pintar(g, u.getHoraInicio(), u.getHoraFin(), EQUIPO, "equipo");
		}
		for (ReservaEntity r : reservas)
			pintar(g, r.getHoraInicio(), r.getHoraFin(), RESERVA, "reservada");
		if (seleccionada != null) {
			g.setColor(SELECCION);
			int x = aX(seleccionada.getHoraInicio());
			g.drawRect(x + 1, 1, aX(seleccionada.getHoraFin()) - x - 3, ALTO_BARRA - 3);
		}
		g.setColor(Color.GRAY);
		g.drawRect(MARGEN_X, 0, ancho(), ALTO_BARRA);

		// Escala de horas cada 2 horas
		FontMetrics fm = g.getFontMetrics();
		for (int h = ReservaModel.APERTURA / 60; h <= ReservaModel.CIERRE / 60; h++) {
			int x = aX(h * 60);
			g.setColor(Color.GRAY);
			g.drawLine(x, ALTO_BARRA, x, ALTO_BARRA + 3);
			if (h % 2 == 0) {
				String txt = h + ":00";
				g.setColor(Color.DARK_GRAY);
				g.drawString(txt, x - fm.stringWidth(txt) / 2, ALTO_BARRA + ALTO_HORAS);
			}
		}
	}

	private void pintar(Graphics2D g, int inicio, int fin, Color color, String texto) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		if (x2 <= x1)
			return;
		g.setColor(color);
		g.fillRect(x1, 0, x2 - x1, ALTO_BARRA);
		g.setColor(Color.WHITE);
		g.drawLine(x2 - 1, 0, x2 - 1, ALTO_BARRA);
		FontMetrics fm = g.getFontMetrics();
		if (!texto.isEmpty() && fm.stringWidth(texto) + 6 < x2 - x1) {
			g.setColor(Color.BLACK);
			g.drawString(texto, x1 + (x2 - x1 - fm.stringWidth(texto)) / 2, (ALTO_BARRA + fm.getAscent()) / 2 - 1);
		}
	}

	private void pintarRayado(Graphics2D g, int inicio, int fin) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		if (x2 > x1)
			rayar(g, x1, 0, x2 - x1, ALTO_BARRA);
	}

	/** Rectangulo blanco con rayas grises en diagonal, el de los huecos de menos de 1 hora */
	private static void rayar(Graphics g, int x, int y, int ancho, int alto) {
		g.setColor(CORTO);
		g.fillRect(x, y, ancho, alto);
		Graphics g2 = g.create(x, y, ancho, alto);
		g2.setColor(CORTO_RAYA);
		for (int i = -alto; i < ancho; i += 8)
			g2.fillPolygon(new int[] { i, i + 3, i + 3 + alto, i + alto }, new int[] { alto, alto, 0, 0 }, 4);
		g2.dispose();
	}

	/** Leyenda de la barra, con muestras pintadas igual que en ella */
	public static JPanel crearLeyenda() {
		JPanel leyenda = new JPanel(new MigLayout("insets 0", "", ""));
		leyenda.add(muestra("Libre", LIBRE, false));
		leyenda.add(muestra("Uso de un equipo", EQUIPO, false), "gapleft 10");
		leyenda.add(muestra("1 h 30 min tras el equipo", MARGEN, false), "gapleft 10");
		leyenda.add(muestra("Otra reserva", RESERVA, false), "gapleft 10");
		leyenda.add(muestra("Hueco de menos de 1 h", CORTO, true), "gapleft 10");
		return leyenda;
	}

	private static JLabel muestra(String texto, Color color, boolean rayado) {
		JLabel lbl = new JLabel(texto, new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				if (rayado)
					rayar(g, x, y, 14, 14);
				else {
					g.setColor(color);
					g.fillRect(x, y, 14, 14);
				}
				g.setColor(Color.GRAY);
				g.drawRect(x, y, 13, 13);
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
