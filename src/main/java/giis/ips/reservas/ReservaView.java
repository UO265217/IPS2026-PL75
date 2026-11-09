package giis.ips.reservas;

import com.toedter.calendar.JDateChooser;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Font;

@Getter
public class ReservaView {
    private JFrame frame;

    private JComboBox<String> cbInstalacion;
    private JDateChooser dcFecha;
    private DisponibilidadPanel panelDisponibilidad;
    private JLabel lblEstado;
    private JSpinner spHoraInicio;
    private JSpinner spMinInicio;
    private JSpinner spHoraFin;
    private JSpinner spMinFin;
    private JTextField txtNombre;
    private JTextField txtTarjeta;
    private JLabel lblImporte;
    private JButton btnReservar;
    private JButton btnCerrar;

    public ReservaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Reserva de instalaciones por usuarios externos");
        frame.setName("Reservas");
        frame.setBounds(0, 0, 860, 520);
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

        // Disponibilidad del dia (pista "Actual") y seccion que se esta reservando (pista "Nueva")
        frame.getContentPane().add(new JLabel("Disponibilidad de 8:00 a 22:00 (pulsa una franja libre para elegirla):"),
                "span 2, left, gaptop 10, wrap");
        panelDisponibilidad = new DisponibilidadPanel();
        panelDisponibilidad.setName("panelDisponibilidad");
        frame.getContentPane().add(panelDisponibilidad, "span 2, growx, wrap");
        frame.getContentPane().add(DisponibilidadPanel.crearLeyenda(), "span 2, left, wrap");
        lblEstado = new JLabel(" ");
        lblEstado.setName("lblEstado");
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblEstado, "span 2, left, gaptop 5, wrap");

        // Datos de la reserva
        JLabel lblDatos = new JLabel("Datos de la reserva");
        lblDatos.setFont(lblDatos.getFont().deriveFont(Font.BOLD));
        frame.getContentPane().add(lblDatos, "span 2, left, gaptop 10, wrap");
        spHoraInicio = new JSpinner();
        spMinInicio = new JSpinner();
        addHora("Hora de inicio:", spHoraInicio, "spHoraInicio", spMinInicio, "spMinInicio");
        spHoraFin = new JSpinner();
        spMinFin = new JSpinner();
        addHora("Hora de fin:", spHoraFin, "spHoraFin", spMinFin, "spMinFin");

        txtNombre = new JTextField();
        txtNombre.setName("txtNombre");
        JLabel lblNombre = new JLabel("Nombre del usuario:");
        lblNombre.setLabelFor(txtNombre);
        frame.getContentPane().add(lblNombre);
        frame.getContentPane().add(txtNombre, "w 300!, wrap");

        txtTarjeta = new JTextField();
        txtTarjeta.setName("txtTarjeta");
        JLabel lblTarjeta = new JLabel("Nº de tarjeta:");
        lblTarjeta.setLabelFor(txtTarjeta);
        frame.getContentPane().add(lblTarjeta);
        frame.getContentPane().add(txtTarjeta, "w 200!, wrap");

        lblImporte = new JLabel(" ");
        lblImporte.setName("lblImporte");
        lblImporte.setFont(lblImporte.getFont().deriveFont(Font.BOLD, 14f));
        frame.getContentPane().add(new JLabel("Importe:"));
        frame.getContentPane().add(lblImporte, "wrap");

        btnCerrar = new JButton("Cancelar");
        btnCerrar.setName("btnCerrar");
        frame.getContentPane().add(btnCerrar, "span 2, split 2, right, gaptop 15");

        btnReservar = new JButton("Reservar");
        btnReservar.setName("btnReservar");
        frame.getContentPane().add(btnReservar);
    }

    // Fila con un spinner para las horas (8-22, el horario de reservas) y otro para los minutos (0-59)
    private void addHora(String etiqueta, JSpinner spHora, String nombreHora, JSpinner spMin, String nombreMin) {
        spHora.setModel(new SpinnerNumberModel(8, 8, 22, 1));
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
