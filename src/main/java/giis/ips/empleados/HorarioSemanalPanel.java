package giis.ips.empleados;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Rejilla semanal al estilo del SIES: una columna por dia, filas de media hora y cada turno dibujado
 * como un bloque que ocupa su franja. Pulsando sobre un bloque se selecciona el turno.
 */
public class HorarioSemanalPanel extends JPanel implements Scrollable {
	private static final long serialVersionUID = 1L;

	private static final String[] DIAS = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo" };
	private static final int HORA_DESDE = 0; // se muestra el dia completo, de 00:00 a 24:00
	private static final int HORA_HASTA = 24;
	private static final int ANCHO_HORAS = 55;
	private static final int ALTO_CABECERA = 26;
	private static final int ALTO_MIN_FRANJA = 12; // alto minimo de cada media hora

	private static final Color GRIS_CABECERA = new Color(0xD3, 0xD3, 0xD3);
	private static final Color LINEA_HORA = new Color(0xB4, 0xB4, 0xB4);
	private static final Color LINEA_MEDIA = new Color(0xCC, 0xCC, 0xCC);
	private static final Color TEXTO_DIA = new Color(0x33, 0x33, 0x33);
	private static final Color TEXTO_FINDE = new Color(0xA0, 0x00, 0x00);
	private static final Color AZUL_HORA = new Color(0x1A, 0x1A, 0x8C);
	private static final Color FONDO_TURNO = new Color(0xC8, 0xE9, 0xFA);
	private static final Color CABECERA_TURNO = new Color(0xAF, 0xE0, 0xF7);
	private static final Color BORDE_SELECCION = new Color(0x1E, 0x64, 0xC8);

	private List<HorarioEntity> turnos = new ArrayList<>();
	private HorarioEntity seleccionado; // turno pulsado o null si no hay ninguno
	private final int desde = HORA_DESDE;
	private final int hasta = HORA_HASTA;

	public HorarioSemanalPanel() {
		setName("panelHorario");
		setBackground(Color.WHITE);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				seleccionado = null;
				for (HorarioEntity h : turnos)
					if (getRectangulo(h).contains(e.getPoint()))
						seleccionado = h;
				repaint();
			}
		});
	}

	/** Cambia los turnos mostrados y quita la seleccion */
	public void setTurnos(List<HorarioEntity> turnos) {
		this.turnos = turnos == null ? new ArrayList<>() : turnos;
		seleccionado = null;
		repaint();
	}

	public HorarioEntity getSeleccionado() {
		return seleccionado;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(ANCHO_HORAS + 7 * 110, ALTO_CABECERA + franjas() * ALTO_MIN_FRANJA);
	}

	// Dentro de un JScrollPane ocupa todo el ancho, y todo el alto si cabe; si no, aparece la barra vertical
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
		return ALTO_MIN_FRANJA;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
		return visible.height;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
	}

	private int franjas() {
		return (hasta - desde) * 2;
	}

	private double anchoDia() {
		return (getWidth() - ANCHO_HORAS) / 7.0;
	}

	private double altoFranja() {
		return Math.max(ALTO_MIN_FRANJA, (getHeight() - ALTO_CABECERA) / (double) franjas());
	}

	/** Coordenada vertical de una hora expresada en minutos desde las 00:00 */
	private int y(int minutos) {
		return ALTO_CABECERA + (int) Math.round((minutos - desde * 60) / 30.0 * altoFranja());
	}

	private int x(int columna) {
		return ANCHO_HORAS + (int) Math.round(columna * anchoDia());
	}

	private Rectangle getRectangulo(HorarioEntity h) {
		int col = h.getDiaSemana() - 1;
		int top = y(h.getHoraInicio());
		return new Rectangle(x(col) + 1, top + 1, x(col + 1) - x(col) - 1, y(h.getHoraFin()) - top - 1);
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0.create();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int fondo = y(hasta * 60);

		// Columna de horas y cabecera de dias en gris
		g.setColor(GRIS_CABECERA);
		g.fillRect(0, 0, ANCHO_HORAS, getHeight());
		g.fillRect(0, 0, getWidth(), ALTO_CABECERA);

		// Lineas horizontales: continua en cada hora y punteada en cada media hora
		Stroke continua = g.getStroke();
		Stroke punteada = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] { 2, 2 }, 0);
		Font fHora = getFont().deriveFont(Font.PLAIN, getFont().getSize2D() + 2);
		for (int m = desde * 60; m <= hasta * 60; m += 30) {
			boolean enPunto = m % 60 == 0;
			g.setColor(enPunto ? LINEA_HORA : LINEA_MEDIA);
			g.setStroke(enPunto ? continua : punteada);
			g.drawLine(enPunto ? 0 : ANCHO_HORAS, y(m), getWidth(), y(m));
			if (enPunto && m < hasta * 60) {
				g.setColor(AZUL_HORA);
				g.setFont(fHora);
				String texto = String.format("%02d:00", m / 60);
				FontMetrics fm = g.getFontMetrics();
				g.drawString(texto, ANCHO_HORAS - 5 - fm.stringWidth(texto), y(m) + fm.getAscent() + 2);
			}
		}
		g.setStroke(continua);

		// Separadores verticales y nombres de los dias (el fin de semana en rojo)
		Font fDia = getFont().deriveFont(Font.BOLD);
		g.setFont(fDia);
		FontMetrics fm = g.getFontMetrics();
		for (int col = 0; col < 7; col++) {
			g.setColor(LINEA_HORA);
			g.drawLine(x(col), 0, x(col), fondo);
			g.setColor(col >= 5 ? TEXTO_FINDE : TEXTO_DIA);
			int centro = (x(col) + x(col + 1)) / 2;
			g.drawString(DIAS[col], centro - fm.stringWidth(DIAS[col]) / 2, (ALTO_CABECERA + fm.getAscent()) / 2 - 1);
		}
		g.setColor(LINEA_HORA);
		g.drawLine(getWidth() - 1, 0, getWidth() - 1, fondo);

		for (HorarioEntity h : turnos)
			pintarTurno(g, h);
		g.dispose();
	}

	/** Bloque del turno: cabecera mas oscura con la franja horaria y debajo la duracion */
	private void pintarTurno(Graphics2D g, HorarioEntity h) {
		Rectangle r = getRectangulo(h);
		Graphics2D gt = (Graphics2D) g.create();
		gt.clipRect(r.x, r.y, r.width, r.height);
		gt.setColor(FONDO_TURNO);
		gt.fill(r);

		Font fPeq = getFont().deriveFont(getFont().getSize2D() - 1);
		gt.setFont(fPeq);
		FontMetrics fm = gt.getFontMetrics();
		int linea = fm.getHeight();
		gt.setColor(CABECERA_TURNO);
		gt.fillRect(r.x, r.y, r.width, linea * 2 + 4);

		gt.setColor(AZUL_HORA);
		gt.drawString(hora(h.getHoraInicio()) + " - " + hora(h.getHoraFin()), r.x + 3, r.y + 2 + fm.getAscent());
		gt.setColor(Color.BLACK);
		gt.drawString(hora(h.getHoraFin() - h.getHoraInicio()) + " h", r.x + 3, r.y + 2 + linea + fm.getAscent());

		if (h == seleccionado) {
			gt.setColor(BORDE_SELECCION);
			gt.setStroke(new BasicStroke(3));
			gt.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
		}
		gt.dispose();
	}

	private static String hora(int minutos) {
		return String.format("%02d:%02d", minutos / 60, minutos % 60);
	}
}
