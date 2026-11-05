package giis.ips.entrenamientos;

import com.toedter.calendar.JDateChooser;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Font;

@Getter
public class EntrenamientoView {
    private JFrame frame;

    private JComboBox<String> cbEquipo;
    private JComboBox<String> cbInstalacion;
    private JDateChooser dcFecha;
    private PrevisualizacionPanel panelPrevisualizacion;
    private JLabel lblEstado;
    private JSpinner spHoraInicio;
    private JSpinner spMinInicio;
    private JSpinner spHoraFin;
    private JSpinner spMinFin;
    private JButton btnAsignar;
    private JButton btnCerrar;

    public EntrenamientoView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Asignar entrenamiento a una instalación");
        frame.setName("Entrenamientos");
        frame.setBounds(0, 0, 860, 480);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", ""));

        cbEquipo = new JComboBox<>();
        cbEquipo.setName("cbEquipo");
        JLabel lblEquipo = new JLabel("Equipo:");
        lblEquipo.setLabelFor(cbEquipo);
        frame.getContentPane().add(lblEquipo);
        frame.getContentPane().add(cbEquipo, "w 250!, wrap");

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

        // Previsualizacion del dia con su leyenda y un resumen de lo que pasara al asignar
        JLabel lblPrevisualizacion = new JLabel("Previsualización");
        lblPrevisualizacion.setFont(lblPrevisualizacion.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblPrevisualizacion, "span 2, left, gaptop 10, wrap");
        panelPrevisualizacion = new PrevisualizacionPanel();
        panelPrevisualizacion.setName("panelPrevisualizacion");
        frame.getContentPane().add(panelPrevisualizacion, "span 2, growx, wrap");
        frame.getContentPane().add(PrevisualizacionPanel.crearLeyenda(), "span 2, left, wrap");
        lblEstado = new JLabel(" ");
        lblEstado.setName("lblEstado");
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblEstado, "span 2, left, gaptop 5, wrap");

        // Horario del entrenamiento
        JLabel lblHorario = new JLabel("Horario del entrenamiento");
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

        btnAsignar = new JButton("Asignar entrenamiento");
        btnAsignar.setName("btnAsignar");
        frame.getContentPane().add(btnAsignar);
        frame.getRootPane().setDefaultButton(btnAsignar);
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
