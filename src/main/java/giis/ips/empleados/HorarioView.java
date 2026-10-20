package giis.ips.empleados;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Color;

@Getter
public class HorarioView {

    private JFrame frame;

    private JTable tabEmpleados;
    private HorarioSemanalPanel panelHorario;
    private JLabel lblTotal;
    private JComboBox<String> cbDia;
    private JSpinner spHoraInicio;
    private JSpinner spMinInicio;
    private JSpinner spHoraFin;
    private JSpinner spMinFin;
    private JButton btnAnyadir;
    private JButton btnEliminar;
    private JButton btnCerrar;

    public HorarioView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Horarios de empleados no deportivos");
        frame.setName("Horarios");
        frame.setBounds(0, 0, 1000, 900);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", "[][100:150][][grow]"));

        frame.getContentPane().add(new JLabel("Selecciona un empleado:"), "span 2, left, wrap");
        tabEmpleados = new JTable();
        tabEmpleados.setName("tabEmpleados");
        tabEmpleados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabEmpleados.setDefaultEditor(Object.class, null); // Tabla de solo lectura
        frame.getContentPane().add(new JScrollPane(tabEmpleados), "span 2, grow, wrap");

        frame.getContentPane().add(new JLabel("Horario semanal por defecto (pulsa un turno para seleccionarlo):"), "span 2, left, wrap");
        panelHorario = new HorarioSemanalPanel();
        JScrollPane spHorario = new JScrollPane(panelHorario);
        spHorario.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        frame.getContentPane().add(spHorario, "span 2, grow, wrap");
        lblTotal = new JLabel(" ");
        lblTotal.setName("lblTotal");
        frame.getContentPane().add(lblTotal, "span 2, left, wrap");

        // Datos del nuevo turno
        cbDia = new JComboBox<>();
        cbDia.setName("cbDia");
        frame.getContentPane().add(new JLabel("Día:"));
        frame.getContentPane().add(cbDia, "growx, wrap");
        spHoraInicio = new JSpinner();
        spMinInicio = new JSpinner();
        addHora("Hora de inicio:", spHoraInicio, "spHoraInicio", spMinInicio, "spMinInicio");
        spHoraFin = new JSpinner();
        spMinFin = new JSpinner();
        addHora("Hora de fin:", spHoraFin, "spHoraFin", spMinFin, "spMinFin");

        btnCerrar = new JButton("Cerrar");
        btnCerrar.setName("btnCerrar");
        frame.getContentPane().add(btnCerrar, "span 2, split 3, right, gaptop 15");

        btnEliminar = new JButton("Eliminar turno seleccionado");
        btnEliminar.setName("btnEliminar");
        frame.getContentPane().add(btnEliminar);

        btnAnyadir = new JButton("Añadir turno");
        btnAnyadir.setName("btnAnyadir");
        frame.getContentPane().add(btnAnyadir);
    }

    // Fila con un spinner para las horas (0-24) y otro para los minutos (0-59)
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
