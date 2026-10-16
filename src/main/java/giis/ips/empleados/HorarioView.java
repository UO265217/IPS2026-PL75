package giis.ips.empleados;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

@Getter
public class HorarioView {

    private JFrame frame;

    private JTable tabEmpleados;
    private JTable tabTurnos;
    private JLabel lblTotal;
    private JComboBox<String> cbDia;
    private JTextField txtInicio;
    private JTextField txtFin;
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
        frame.setBounds(0, 0, 620, 620);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", "[][grow][][grow]"));

        frame.getContentPane().add(new JLabel("Selecciona un empleado:"), "span 2, left, wrap");
        tabEmpleados = new JTable();
        tabEmpleados.setName("tabEmpleados");
        tabEmpleados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabEmpleados.setDefaultEditor(Object.class, null); // Tabla de solo lectura
        frame.getContentPane().add(new JScrollPane(tabEmpleados), "span 2, grow, wrap");

        frame.getContentPane().add(new JLabel("Horario semanal por defecto:"), "span 2, left, wrap");
        tabTurnos = new JTable();
        tabTurnos.setName("tabTurnos");
        tabTurnos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabTurnos.setDefaultEditor(Object.class, null);
        frame.getContentPane().add(new JScrollPane(tabTurnos), "span 2, grow, wrap");
        lblTotal = new JLabel(" ");
        lblTotal.setName("lblTotal");
        frame.getContentPane().add(lblTotal, "span 2, left, wrap");

        // Datos del nuevo turno
        cbDia = new JComboBox<>();
        cbDia.setName("cbDia");
        frame.getContentPane().add(new JLabel("Día:"));
        frame.getContentPane().add(cbDia, "growx, wrap");
        txtInicio = addCampo("Hora de inicio (HH:mm):", "txtInicio");
        txtFin = addCampo("Hora de fin (HH:mm):", "txtFin");

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

    // Mediante la etiqueta y el nombre crea el txtField
    private JTextField addCampo(String etiqueta, String nombre) {
        JLabel lbl = new JLabel(etiqueta);
        JTextField txt = new JTextField(20);
        txt.setName(nombre);
        lbl.setLabelFor(txt);
        frame.getContentPane().add(lbl);
        frame.getContentPane().add(txt, "growx, wrap");
        return txt;
    }
}
