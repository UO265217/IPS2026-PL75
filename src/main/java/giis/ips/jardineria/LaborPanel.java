package giis.ips.jardineria;

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

import giis.ips.entrenamientos.UsoInstalacionDTO;
import giis.ips.reservas.FranjaDTO;
import net.miginfocom.swing.MigLayout;

/**
 * Previsualizacion del dia al programar una labor de jardineria (HU 36125), con dos pistas:
 * - "Instalacion": lo que ya hay en ella (entrenamientos, partidos, reservas externas y labores de otros
 *   jardineros), con el horario elegido recuadrado en azul.
 * - "Jardinero": de fondo sus turnos en verde claro; encima sus labores de ese dia (lisas si son en esta instalacion
 *   y rayadas si son en otra) y la labor nueva, en verde oscuro si se puede programar y en gris si no.
 * Lo que impide programar la labor lleva borde negro grueso. Se dibuja de las 6:00 a las 24:00, o desde antes si
 * algo empieza mas temprano.
 */
public class LaborPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final int ALTO_PISTA = 30;
	private static final int SEPARACION = 6;
	private static final int ALTO_HORAS = 16;
	private static final int ANCHO_ETIQUETA = 76;
	private static final int MARGEN_DER = 16;
	private static final int INICIO_DEFECTO = 6 * 60;

	private static final Color ENTRENAMIENTO = new Color(0xE5, 0x73, 0x73);
	private static final Color PARTIDO = new Color(0xC6, 0x28, 0x28);
	private static final Color RESERVA = new Color(0x7F, 0xA7, 0xDD);
	private static final Color JARDINERIA = new Color(0xA1, 0x88, 0x7F);
	private static final Color JARDINERIA_RAYA = new Color(0xCB, 0xBA, 0xB4);
	private static final Color TURNO = new Color(0xDC, 0xEF, 0xDC);
	private static final Color TURNO_BORDE = new Color(0x8B, 0xC3, 0x8B);
	private static final Color NUEVA = new Color(0x2E, 0x7D, 0x32);
	private static final Color NUEVA_IMPOSIBLE = new Color(0x9E, 0x9E, 0x9E);
	private static final Color SELECCION = new Color(0x1E, 0x64, 0xC8);

	private List<UsoInstalacionDTO> usos = new ArrayList<>();
	private List<FranjaDTO> turnos = new ArrayList<>();
	private List<LaborJardineroDTO> labores = new ArrayList<>();
	private int instalacionId;
	private int nuevaInicio;
	private int nuevaFin;
	private boolean nuevaPosible;

	public LaborPanel() {
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(780, 2 * ALTO_PISTA + SEPARACION + ALTO_HORAS + 4));
	}

	/**
	 * Datos a mostrar: usos de la instalacion, turnos y labores del jardinero (cualquier lista puede ser null), la
	 * instalacion elegida (para distinguir las labores en otra) y la labor nueva. Si su fin no es posterior al inicio
	 * no se dibuja
	 */
	public void setDatos(List<UsoInstalacionDTO> usos, List<FranjaDTO> turnos, List<LaborJardineroDTO> labores,
			int instalacionId, int nuevaInicio, int nuevaFin, boolean nuevaPosible) {
		this.usos = usos == null ? new ArrayList<>() : usos;
		this.turnos = turnos == null ? new ArrayList<>() : turnos;
		this.labores = labores == null ? new ArrayList<>() : labores;
		this.instalacionId = instalacionId;
		this.nuevaInicio = nuevaInicio;
		this.nuevaFin = nuevaFin;
		this.nuevaPosible = nuevaPosible;
		repaint();
	}

	private boolean hayNueva() {
		return nuevaFin > nuevaInicio;
	}

	private int inicioEscala() {
		int inicio = INICIO_DEFECTO;
		for (UsoInstalacionDTO u : usos)
			inicio = Math.min(inicio, u.getHoraInicio());
		for (FranjaDTO t : turnos)
			inicio = Math.min(inicio, t.getHoraInicio());
		for (LaborJardineroDTO l : labores)
			inicio = Math.min(inicio, l.getHoraInicio());
		if (hayNueva())
			inicio = Math.min(inicio, Math.max(0, nuevaInicio));
		return inicio / 60 * 60;
	}

	private int aX(int minuto) {
		int inicio = inicioEscala();
		int ancho = getWidth() - ANCHO_ETIQUETA - MARGEN_DER;
		int m = Math.max(inicio, Math.min(JardineriaModel.FIN_DIA, minuto));
		return ANCHO_ETIQUETA + (m - inicio) * ancho / (JardineriaModel.FIN_DIA - inicio);
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
		FontMetrics fm = g.getFontMetrics();
		int yJard = ALTO_PISTA + SEPARACION;

		// Pista "Instalacion"
		for (UsoInstalacionDTO u : usos)
			pintar(g, 0, u.getHoraInicio(), u.getHoraFin(), color(u), null, u.getDescripcion(), Color.WHITE);

		// Pista "Jardinero": turnos de fondo, sus labores y la nueva encima
		for (FranjaDTO t : turnos) {
			pintar(g, yJard, t.getHoraInicio(), t.getHoraFin(), TURNO, null, "", Color.BLACK);
			g.setColor(TURNO_BORDE);
			g.drawLine(aX(t.getHoraInicio()), yJard, aX(t.getHoraInicio()), yJard + ALTO_PISTA);
			g.drawLine(aX(t.getHoraFin()) - 1, yJard, aX(t.getHoraFin()) - 1, yJard + ALTO_PISTA);
		}
		for (LaborJardineroDTO l : labores) {
			boolean otra = l.getInstalacionId() != instalacionId;
			pintar(g, yJard, l.getHoraInicio(), l.getHoraFin(), JARDINERIA, otra ? JARDINERIA_RAYA : null,
					otra ? l.getInstalacion() : l.getDescripcion(), otra ? Color.BLACK : Color.WHITE);
		}

		if (hayNueva()) {
			String txt = nuevaPosible ? hora(nuevaInicio) + " - " + hora(nuevaFin) : "No se puede";
			if (fm.stringWidth(txt) + 6 >= aX(nuevaFin) - aX(nuevaInicio))
				txt = nuevaPosible ? hora(nuevaInicio) + "-" + hora(nuevaFin) : "No";
			if (fm.stringWidth(txt) + 6 >= aX(nuevaFin) - aX(nuevaInicio))
				txt = nuevaPosible ? "" : "No"; // Las horas recortadas confunden: mejor sin texto
			pintar(g, yJard, nuevaInicio, nuevaFin, nuevaPosible ? NUEVA : NUEVA_IMPOSIBLE, null, txt, Color.WHITE);

			// Lo que impide programarla, con borde negro, y el horario elegido recuadrado sobre la instalacion
			for (UsoInstalacionDTO u : JardineriaModel.getConflictosInstalacion(usos, nuevaInicio, nuevaFin))
				borde(g, 0, u.getHoraInicio(), u.getHoraFin(), Color.BLACK, 3);
			for (LaborJardineroDTO l : JardineriaModel.getConflictosJardinero(labores, nuevaInicio, nuevaFin))
				borde(g, yJard, l.getHoraInicio(), l.getHoraFin(), Color.BLACK, 3);
			borde(g, 0, nuevaInicio, nuevaFin, SELECCION, 2);
		}

		// Marcos y etiquetas de las pistas
		int ancho = aX(JardineriaModel.FIN_DIA) - ANCHO_ETIQUETA;
		g.setColor(Color.GRAY);
		g.drawRect(ANCHO_ETIQUETA, 0, ancho, ALTO_PISTA);
		g.drawRect(ANCHO_ETIQUETA, yJard, ancho, ALTO_PISTA);
		g.setColor(Color.DARK_GRAY);
		g.drawString("Instalación", ANCHO_ETIQUETA - 6 - fm.stringWidth("Instalación"),
				(ALTO_PISTA + fm.getAscent()) / 2 - 1);
		g.drawString("Jardinero", ANCHO_ETIQUETA - 6 - fm.stringWidth("Jardinero"),
				yJard + (ALTO_PISTA + fm.getAscent()) / 2 - 1);

		// Escala de horas cada 2 horas
		int yHoras = yJard + ALTO_PISTA;
		for (int h = inicioEscala() / 60; h <= JardineriaModel.FIN_DIA / 60; h++) {
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

	/** Bloque de color (rayado si se indica raya) con el texto centrado si cabe; si no, recortado o nada */
	private void pintar(Graphics2D g, int y, int inicio, int fin, Color color, Color raya, String txt, Color colorTxt) {
		int x1 = aX(inicio);
		int x2 = aX(fin);
		if (x2 <= x1)
			return;
		rayar(g, x1, y, x2 - x1, ALTO_PISTA, color, raya);
		g.setColor(Color.WHITE);
		g.drawLine(x2 - 1, y, x2 - 1, y + ALTO_PISTA);
		FontMetrics fm = g.getFontMetrics();
		String t = txt == null ? "" : txt;
		while (!t.isEmpty() && fm.stringWidth(t) + 6 > x2 - x1)
			t = t.length() <= 2 ? "" : t.substring(0, t.length() - 2) + "…";
		if (t.isEmpty() || t.equals("…"))
			return;
		g.setColor(colorTxt);
		g.drawString(t, x1 + (x2 - x1 - fm.stringWidth(t)) / 2, y + (ALTO_PISTA + fm.getAscent()) / 2 - 1);
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

	/** Rectangulo de color; con rayas en diagonal si raya no es null */
	private static void rayar(Graphics g, int x, int y, int ancho, int alto, Color fondo, Color raya) {
		if (ancho <= 0)
			return;
		g.setColor(fondo);
		g.fillRect(x, y, ancho, alto);
		if (raya == null)
			return;
		Graphics g2 = g.create(x, y, ancho, alto);
		g2.setColor(raya);
		for (int i = -alto; i < ancho; i += 10)
			g2.fillPolygon(new int[] { i, i + 5, i + 5 + alto, i + alto }, new int[] { alto, alto, 0, 0 }, 4);
		g2.dispose();
	}

	/** Leyenda de la previsualizacion, con muestras pintadas igual que en ella */
	public static JPanel crearLeyenda() {
		JPanel leyenda = new JPanel(new MigLayout("insets 0, wrap 5", "", ""));
		leyenda.add(muestra("Entrenamiento", ENTRENAMIENTO, null, null));
		leyenda.add(muestra("Partido", PARTIDO, null, null), "gapleft 10");
		leyenda.add(muestra("Reserva externa", RESERVA, null, null), "gapleft 10");
		leyenda.add(muestra("Labor en esta instalación", JARDINERIA, null, null), "gapleft 10");
		leyenda.add(muestra("Labor en otra instalación", JARDINERIA, JARDINERIA_RAYA, null), "gapleft 10");
		leyenda.add(muestra("Turno del jardinero", TURNO, null, TURNO_BORDE));
		leyenda.add(muestra("Nueva labor", NUEVA, null, null), "gapleft 10");
		leyenda.add(muestra("No se puede programar", NUEVA_IMPOSIBLE, null, null), "gapleft 10");
		leyenda.add(muestra("Horario elegido", Color.WHITE, null, SELECCION), "gapleft 10");
		leyenda.add(muestra("Impide programar", ENTRENAMIENTO, null, Color.BLACK), "gapleft 10");
		return leyenda;
	}

	private static JLabel muestra(String texto, Color color, Color raya, Color borde) {
		JLabel lbl = new JLabel(texto, new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				rayar(g, x, y, 14, 14, color, raya);
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
