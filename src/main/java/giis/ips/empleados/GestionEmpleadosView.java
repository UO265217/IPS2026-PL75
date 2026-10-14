package giis.ips.empleados;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

@Getter
public class GestionEmpleadosView {

    private JFrame frame;

    private JTable tabEmpleados;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtDni;
    private JTextField txtTelefono;
    private JTextField txtFechaNacimiento;
    private JTextField txtTipo;
    private JTextField txtPosicion;
    private JTextField txtSalario;
    private JButton btnGuardar;
    private JButton btnEliminar;
    private JButton btnCerrar;

    public GestionEmpleadosView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Modificar y eliminar empleados");
        frame.setName("GestionEmpleados");
        frame.setBounds(0, 0, 620, 620);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", "[][grow]")); // 2 columnas, la tabla crece

        frame.getContentPane().add(new JLabel("Selecciona un empleado:"), "span 2, left, wrap");
        tabEmpleados = new JTable();
        tabEmpleados.setName("tabEmpleados");
        tabEmpleados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabEmpleados.setDefaultEditor(Object.class, null); // Tabla de solo lectura
        frame.getContentPane().add(new JScrollPane(tabEmpleados), "span 2, grow, wrap");

        // Datos personales y profesionales que se pueden modificar
        txtNombre = addCampo("Nombre:", "txtNombre");
        txtApellido = addCampo("Apellido:", "txtApellido");
        txtDni = addCampo("DNI:", "txtDni");
        txtTelefono = addCampo("Teléfono:", "txtTelefono");
        txtFechaNacimiento = addCampo("Fecha de nacimiento (aaaa-mm-dd):", "txtFechaNacimiento");
        txtSalario = addCampo("Salario bruto anual:", "txtSalario");

        // El puesto de trabajo solo se muestra: no se puede cambiar
        txtTipo = addCampo("Tipo de empleado:", "txtTipo");
        txtTipo.setEditable(false);
        txtPosicion = addCampo("Posición:", "txtPosicion");
        txtPosicion.setEditable(false);

        btnCerrar = new JButton("Cerrar");
        btnCerrar.setName("btnCerrar");
        frame.getContentPane().add(btnCerrar, "span 2, split 3, right, gaptop 15");

        btnEliminar = new JButton("Eliminar empleado");
        btnEliminar.setName("btnEliminar");
        frame.getContentPane().add(btnEliminar);

        btnGuardar = new JButton("Guardar cambios");
        btnGuardar.setName("btnGuardar");
        frame.getContentPane().add(btnGuardar);
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
