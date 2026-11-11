package giis.ips.jardineria;

import com.toedter.calendar.JDateChooser;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Font;

@Getter
public class JardineriaView {
    private JFrame frame;

    private JComboBox<String> cbInstalacion;
    private JDateChooser dcFecha;
    private JComboBox<String> cbJardinero;
    private JLabel lblTurnos;
    private JTextField txtDescripcion;
    private LaborPanel panelPrevisualizacion;
    private JLabel lblEstado;
    private JSpinner spHoraInicio;
    private JSpinner spMinInicio;
    private JSpinner spHoraFin;
    private JSpinner spMinFin;
    private JButton btnProgramar;
    private JButton btnCerrar;

    public JardineriaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Programar labor de jardinería");
        frame.setName("Jardineria");
        frame.setBounds(0, 0, 900, 540);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", ""));

        cbInstalacion = new JComboBox<>();
        cbInstalacion.setName("cbInstalacion");
        JLabel lblInstalacion = new JLabel("Instalación:");
        lblInstalacion.setLabelFor(cbInstalacion);
        frame.getContentPane().add(lblInstalacion);
        frame.getContentPane().add(cbInstalacion, "w 250!, wrap");

        dcFecha = new JDateChooser();
        dcFecha.setDateFormatString("yyyy-MM-dd");
        dcFecha.setName("dcFecha");
        frame.getContentPane().add(new JLabel("Fecha:"));
        frame.getContentPane().add(dcFecha, "w 150!, wrap");

        cbJardinero = new JComboBox<>();
        cbJardinero.setName("cbJardinero");
        JLabel lblJardinero = new JLabel("Jardinero:");
        lblJardinero.setLabelFor(cbJardinero);
        frame.getContentPane().add(lblJardinero);
        frame.getContentPane().add(cbJardinero, "split 2, w 250!");
        lblTurnos = new JLabel(" ");
        lblTurnos.setName("lblTurnos");
        frame.getContentPane().add(lblTurnos, "gapleft 10, wrap");

        txtDescripcion = new JTextField();
        txtDescripcion.setName("txtDescripcion");
        JLabel lblDescripcion = new JLabel("Descripción:");
        lblDescripcion.setLabelFor(txtDescripcion);
        frame.getContentPane().add(lblDescripcion);
        frame.getContentPane().add(txtDescripcion, "w 350!, wrap");

        // Previsualizacion del dia con su leyenda y si se puede programar
        JLabel lblPrevisualizacion = new JLabel("Previsualización");
        lblPrevisualizacion.setFont(lblPrevisualizacion.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblPrevisualizacion, "span 2, left, gaptop 10, wrap");
        panelPrevisualizacion = new LaborPanel();
        panelPrevisualizacion.setName("panelPrevisualizacion");
        frame.getContentPane().add(panelPrevisualizacion, "span 2, growx, wrap");
        frame.getContentPane().add(LaborPanel.crearLeyenda(), "span 2, left, wrap");
        lblEstado = new JLabel(" ");
        lblEstado.setName("lblEstado");
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblEstado, "span 2, left, gaptop 5, wrap");

        // Horario de la labor
        JLabel lblHorario = new JLabel("Horario de la labor");
        lblHorario.setFont(lblHorario.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblHorario, "span 2, left, gaptop 10, wrap");
        spHoraInicio = new JSpinner();
        spMinInicio = new JSpinner();
        addHora("Hora de inicio:", spHoraInicio, "spHoraInicio", spMinInicio, "spMinInicio");
        spHoraFin = new JSpinner();
        spMinFin = new JSpinner();
        addHora("Hora de fin:", spHoraFin, "spHoraFin", spMinFin, "spMinFin");

        btnCerrar = new JButton("Cancelar");
        btnCerrar.setName("btnCerrar");
        frame.getContentPane().add(btnCerrar, "span 2, split 2, right, gaptop 15");

        btnProgramar = new JButton("Programar labor");
        btnProgramar.setName("btnProgramar");
        frame.getContentPane().add(btnProgramar);
        frame.getRootPane().setDefaultButton(btnProgramar);
    }

    // Fila con un spinner para las horas (0-24, 24 solo vale como fin con 0 minutos) y otro para los minutos (0-59)
    private void addHora(String etiqueta, JSpinner spHora, String nombreHora, JSpinner spMin, String nombreMin) {
        spHora.setModel(new SpinnerNumberModel(0, 0, 24, 1));
        spHora.setName(nombreHora);
        spMin.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        spMin.setName(nombreMin);
        JLabel lbl = new JLabel(etiqueta);
        lbl.setLabelFor(spHora);
        frame.getContentPane().add(lbl);
        frame.getContentPane().add(spHora, "split 4, w 60!");
        frame.getContentPane().add(new JLabel("h"));
        frame.getContentPane().add(spMin, "w 60!");
        frame.getContentPane().add(new JLabel("min"), "wrap");
    }
}
