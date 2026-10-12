package giis.ips.empleados;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

@Getter
public class AltaEmpleadoView {

    private JFrame frame;

    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtDni;
    private JTextField txtTelefono;
    private JTextField txtFechaNacimiento;
    private JComboBox<String> cbTipo;
    private JComboBox<String> cbPosicion;
    private JTextField txtSalario;
    private JButton btnGuardar;
    private JButton btnCancelar;

    public AltaEmpleadoView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Alta de empleado");
        frame.setName("AltaEmpleado");
        frame.setBounds(0, 0, 450, 420);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", "")); // Crea 2 columnas y la derecha crece

        // Crea los campos de texto
        txtNombre = addCampo("Nombre:", "txtNombre");
        txtApellido = addCampo("Apellido:", "txtApellido");
        txtDni = addCampo("DNI:", "txtDni");
        txtTelefono = addCampo("Teléfono:", "txtTelefono");
        txtFechaNacimiento = addCampo("Fecha de nacimiento (aaaa-mm-dd):", "txtFechaNacimiento");

        frame.getContentPane().add(new JLabel("Tipo de empleado:"));
        cbTipo = new JComboBox<>();
        cbTipo.setName("cbTipo");
        frame.getContentPane().add(cbTipo, "growx, wrap");

        frame.getContentPane().add(new JLabel("Posición:"));
        cbPosicion = new JComboBox<>();
        cbPosicion.setName("cbPosicion");
        frame.getContentPane().add(cbPosicion, "growx, wrap");

        txtSalario = addCampo("Salario bruto anual:", "txtSalario");

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setName("btnCancelar");
        frame.getContentPane().add(btnCancelar, "span 2, split 2, right, gaptop 15");

        btnGuardar = new JButton("Guardar");
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
