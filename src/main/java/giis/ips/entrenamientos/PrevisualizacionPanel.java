package giis.ips.entrenamientos;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Previsualizacion del dia de una instalacion al asignar un entrenamiento (HU 36124), con dos pistas:
 * - "Actual": lo que ya hay (entrenamientos, partidos, reservas externas y labores de jardineria), con lo que se
 *   eliminara rayado y con borde rojo y los usos de equipos que impiden asignar con borde negro grueso.
 * - "Nuevo": el entrenamiento que se esta asignando (gris si no se puede) y la hora y media siguiente, en la que
 *   se eliminan las reservas externas que empiecen.
 * Se dibuja de las 6:00 a las 24:00, o desde antes si algo empieza mas temprano.
 */
public class PrevisualizacionPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final int ALTO_PISTA = 30;
	private static final int SEPARACION = 6;
	private static final int ALTO_HORAS = 16;
	private static final int ANCHO_ETIQUETA = 60;
	private static final int MARGEN_DER = 16;
	private static final int INICIO_DEFECTO = 6 * 60;

	private static final Color ENTRENAMIENTO = new Color(0xE5, 0x73, 0x73);
	private static final Color PARTIDO = new Color(0xC6, 0x28, 0x28);
	private static final Color RESERVA = new Color(0x7F, 0xA7, 0xDD);
	private static final Color JARDINERIA = new Color(0xA1, 0x88, 0x7F);
	private static final Color NUEVO = new Color(0x2E, 0x7D, 0x32);
	private static final Color NUEVO_IMPOSIBLE = new Color(0x9E, 0x9E, 0x9E);
	private static final Color MARGEN = new Color(0xFF, 0xD5, 0x4F);
	private static final Color MARGEN_RAYA = new Color(0xFF, 0xE9, 0xA0);
	private static final Color SE_ELIMINA = new Color(0xC6, 0x28, 0x28);
	private static final Color RAYA_ELIMINA = new Color(0xFF, 0xFF, 0xFF, 0x99);

	private List<UsoInstalacionDTO> usos = new ArrayList<>();
	private List<UsoInstalacionDTO> afectados = new ArrayList<>();
	private List<UsoInstalacionDTO> conflictos = new ArrayList<>();
	private int nuevoInicio;
	private int nuevoFin;

	public PrevisualizacionPanel() {
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(760, 2 * ALTO_PISTA + SEPARACION + ALTO_HORAS + 4));
	}

	/**
	 * Datos a mostrar; usos null deja la previsualizacion vacia. Si el fin no es posterior al inicio no se dibuja el
	 * nuevo entrenamiento
	 */
	public void setDatos(List<UsoInstalacionDTO> usos, List<UsoInstalacionDTO> afectados,
			List<UsoInstalacionDTO> conflictos, int nuevoInicio, int nuevoFin) {
		this.usos = usos == null ? new ArrayList<>() : usos;
		this.afectados = afectados == null ? new ArrayList<>() : afectados;
		this.conflictos = conflictos == null ? new ArrayList<>() : conflictos;
		this.nuevoInicio = nuevoInicio;
		this.nuevoFin = usos == null ? nuevoInicio : nuevoFin;
		repaint();
	}

	private int inicioEscala() {
		int inicio = INICIO_DEFECTO;
		for (UsoInstalacionDTO u : usos)
			inicio = Math.min(inicio, u.getHoraInicio());
		if (nuevoFin > nuevoInicio)
			inicio = Math.min(inicio, nuevoInicio);
		return inicio / 60 * 60;
	}

	private int aX(int minuto) {
		int inicio = inicioEscala();
		int ancho = getWidth() - ANCHO_ETIQUETA - MARGEN_DER;
		int m = Math.max(inicio, Math.min(EntrenamientoModel.FIN_DIA, minuto));
		return ANCHO_ETIQUETA + (m - inicio) * ancho / (EntrenamientoModel.FIN_DIA - inicio);
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
		FontMetrics fm = g.getFontMetrics();
		int yNuevo = ALTO_PISTA + SEPARACION;

		// Pista "Actual"
		for (UsoInstalacionDTO u : usos) {
			boolean seElimina = afectados.contains(u);
			pintar(g, 0, u.getHoraInicio(), u.getHoraFin(), color(u), u.getDescripcion(), seElimina);
			if (seElimina)
				borde(g, 0, u.getHoraInicio(), u.getHoraFin(), SE_ELIMINA, 3);
			else if (conflictos.contains(u))
				borde(g, 0, u.getHoraInicio(), u.getHoraFin(), Color.BLACK, 3);
		}

		// Pista "Nuevo"
		if (nuevoFin > nuevoInicio) {
			int finMargen = Math.min(nuevoFin + EntrenamientoModel.MARGEN_TRAS_EQUIPO, EntrenamientoModel.FIN_DIA);
			if (finMargen > nuevoFin) {
				int x1 = aX(nuevoFin);
				rayar(g, x1, yNuevo, aX(finMargen) - x1, ALTO_PISTA, MARGEN, MARGEN_RAYA);
				texto(g, x1, aX(finMargen), yNuevo, "+1h30", Color.DARK_GRAY);
			}
			pintar(g, yNuevo, nuevoInicio, nuevoFin, conflictos.isEmpty() ? NUEVO : NUEVO_IMPOSIBLE,
					conflictos.isEmpty() ? "Nuevo" : "No se puede", false);
		}

		// Marcos y etiquetas de las pistas
		g.setColor(Color.GRAY);
		g.drawRect(ANCHO_ETIQUETA, 0, aX(EntrenamientoModel.FIN_DIA) - ANCHO_ETIQUETA, ALTO_PISTA);
		g.drawRect(ANCHO_ETIQUETA, yNuevo, aX(EntrenamientoModel.FIN_DIA) - ANCHO_ETIQUETA, ALTO_PISTA);
		g.setColor(Color.DARK_GRAY);
		g.drawString("Actual", ANCHO_ETIQUETA - 6 - fm.stringWidth("Actual"), (ALTO_PISTA + fm.getAscent()) / 2 - 1);
		g.drawString("Nuevo", ANCHO_ETIQUETA - 6 - fm.stringWidth("Nuevo"),
				yNuevo + (ALTO_PISTA + fm.getAscent()) / 2 - 1);

		// Escala de horas cada 2 horas
		int yHoras = yNuevo + ALTO_PISTA;
		for (int h = inicioEscala() / 60; h <= EntrenamientoModel.FIN_DIA / 60; h++) {
			int x = aX(h * 60);
			g.setColor(Color.GRAY);
			g.drawLine(x, yHoras, x, yHoras + 3);
			if (h % 2 == 0) {
				String txt = h + ":00";
				g.setColor(Color.DARK_GRAY);
				g.drawString(txt, x - fm.stringWidth(txt) / 2, yHoras + ALTO_HORAS);
			}
		}
	}

	private static Color color(UsoInstalacionDTO u) {
		switch (u.getTipo()) {
		case UsoInstalacionDTO.ENTRENAMIENTO:
			return ENTRENAMIENTO;
		case UsoInstalacionDTO.PARTIDO:
			return PARTIDO;
		case UsoInstalacionDTO.RESERVA:
			return RESERVA;
		default:
			return JARDINERIA;
		}
	}

	private void pintar(Graphics2D g, int y, int inicio, int fin, Color color, String txt, boolean rayado) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		if (x2 <= x1)
			return;
		if (rayado)
			rayar(g, x1, y, x2 - x1, ALTO_PISTA, color, RAYA_ELIMINA);
		else {
			g.setColor(color);
			g.fillRect(x1, y, x2 - x1, ALTO_PISTA);
		}
		g.setColor(Color.WHITE);
		g.drawLine(x2 - 1, y, x2 - 1, y + ALTO_PISTA);
		texto(g, x1, x2, y, txt, rayado ? Color.BLACK : Color.WHITE);
	}

	/** Texto centrado en el bloque si cabe; si no, recortado con puntos suspensivos o nada */
	private static void texto(Graphics2D g, int x1, int x2, int y, String txt, Color color) {
		FontMetrics fm = g.getFontMetrics();
		String t = txt == null ? "" : txt;
		while (!t.isEmpty() && fm.stringWidth(t) + 6 > x2 - x1)
			t = t.length() <= 2 ? "" : t.substring(0, t.length() - 2) + "…";
		if (t.isEmpty() || t.equals("…"))
			return;
		g.setColor(color);
		g.drawString(t, x1 + (x2 - x1 - fm.stringWidth(t)) / 2, y + (ALTO_PISTA + fm.getAscent()) / 2 - 1);
	}

	private void borde(Graphics2D g, int y, int inicio, int fin, Color color, int grosor) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		g.setColor(color);
		g.setStroke(new BasicStroke(grosor));
		g.drawRect(x1 + grosor / 2, y + grosor / 2, x2 - x1 - grosor, ALTO_PISTA - grosor);
		g.setStroke(new BasicStroke(1));
	}

	/** Rectangulo con rayas en diagonal */
	private static void rayar(Graphics g, int x, int y, int ancho, int alto, Color fondo, Color raya) {
		if (ancho <= 0)
			return;
		g.setColor(fondo);
		g.fillRect(x, y, ancho, alto);
		Graphics g2 = g.create(x, y, ancho, alto);
		g2.setColor(raya);
		for (int i = -alto; i < ancho; i += 10)
			g2.fillPolygon(new int[] { i, i + 5, i + 5 + alto, i + alto }, new int[] { alto, alto, 0, 0 }, 4);
		g2.dispose();
	}

	/** Leyenda de la previsualizacion, con muestras pintadas igual que en ella */
	public static JPanel crearLeyenda() {
		JPanel leyenda = new JPanel(new MigLayout("insets 0, wrap 4", "", ""));
		leyenda.add(muestra("Entrenamiento", ENTRENAMIENTO, null, null));
		leyenda.add(muestra("Partido", PARTIDO, null, null), "gapleft 10");
		leyenda.add(muestra("Reserva externa", RESERVA, null, null), "gapleft 10");
		leyenda.add(muestra("Jardinería", JARDINERIA, null, null), "gapleft 10");
		leyenda.add(muestra("Nuevo entrenamiento", NUEVO, null, null));
		leyenda.add(muestra("1 h 30 min sin reservas externas", MARGEN, MARGEN_RAYA, null), "gapleft 10");
		leyenda.add(muestra("Se eliminará", RESERVA, RAYA_ELIMINA, SE_ELIMINA), "gapleft 10");
		leyenda.add(muestra("Impide asignar", ENTRENAMIENTO, null, Color.BLACK), "gapleft 10");
		return leyenda;
	}

	private static JLabel muestra(String texto, Color color, Color raya, Color borde) {
		JLabel lbl = new JLabel(texto, new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				if (raya != null)
					rayar(g, x, y, 14, 14, color, raya);
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
