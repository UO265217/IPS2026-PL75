package giis.ips.empleados;

import com.toedter.calendar.JDateChooser;
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
    private JDateChooser dcFechaNacimiento;
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
        dcFechaNacimiento = addFecha("Fecha de nacimiento:", "dcFechaNacimiento");
        txtSalario = addCampo("Salario bruto anual:", "txtSalario");

        // El puesto de trabajo solo se muestra difuminado: no se puede cambiar
        txtTipo = addCampoNoEditable("Tipo de empleado:", "txtTipo");
        txtPosicion = addCampoNoEditable("Posición:", "txtPosicion");
        JLabel lblAviso = new JLabel("El puesto de trabajo no se puede modificar");
        lblAviso.setEnabled(false);
        lblAviso.setFont(lblAviso.getFont().deriveFont(java.awt.Font.ITALIC));
        frame.getContentPane().add(lblAviso, "skip, wrap");

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

    // Mediante la etiqueta y el nombre crea el selector de fecha (se muestra como aaaa-mm-dd)
    private JDateChooser addFecha(String etiqueta, String nombre) {
        JLabel lbl = new JLabel(etiqueta);
        JDateChooser dc = new JDateChooser();
        dc.setDateFormatString("yyyy-MM-dd");
        dc.setName(nombre);
        lbl.setLabelFor(dc);
        frame.getContentPane().add(lbl);
        frame.getContentPane().add(dc, "growx, wrap");
        return dc;
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

    // Campo que solo se muestra: etiqueta y texto deshabilitados (en gris) y con aviso al pasar el raton
    private JTextField addCampoNoEditable(String etiqueta, String nombre) {
        String aviso = "El puesto de trabajo no se puede modificar";
        JLabel lbl = new JLabel(etiqueta);
        JTextField txt = new JTextField(20);
        txt.setName(nombre);
        lbl.setLabelFor(txt);
        txt.setEnabled(false);
        lbl.setEnabled(false);
        txt.setToolTipText(aviso);
        lbl.setToolTipText(aviso);
        frame.getContentPane().add(lbl);
        frame.getContentPane().add(txt, "growx, wrap");
        return txt;
    }
}
