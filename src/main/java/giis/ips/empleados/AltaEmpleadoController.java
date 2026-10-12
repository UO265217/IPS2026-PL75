package giis.ips.empleados;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador del alta de empleados: instala los manejadores de la vista,
 * realiza las validaciones sintacticas y llama al modelo.
 */
public class AltaEmpleadoController {
	private static final String LETRAS_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";

	private AltaEmpleadoModel model;
	private AltaEmpleadoView view;

	public AltaEmpleadoController(AltaEmpleadoModel m, AltaEmpleadoView v) {
		this.model = m;
		this.view = v;
		this.initView();
	}

	/** Inicializacion del controlador: anyade los manejadores de eventos */
	public void initController() {
		view.getCbTipo().addActionListener(e -> SwingUtil.exceptionWrapper(() -> cargarPosiciones()));
		view.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> guardar()));
		view.getBtnCancelar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		DefaultComboBoxModel<String> tipos = new DefaultComboBoxModel<>();
		for (TipoEmpleado t : TipoEmpleado.values())
			tipos.addElement(t.getNombre());
		view.getCbTipo().setModel(tipos);
		cargarPosiciones();
		view.getFrame().setVisible(true);
	}

	/** Al cambiar el tipo de empleado solo se ofrecen las posiciones de ese tipo */
	public void cargarPosiciones() {
		TipoEmpleado tipo = TipoEmpleado.fromNombre((String) view.getCbTipo().getSelectedItem());
		view.getCbPosicion().setModel(new DefaultComboBoxModel<>(Posicion.nombresDeTipo(tipo).toArray(new String[0])));
	}

	public void guardar() {
		EmpleadoEntity empleado = crearEmpleado(view.getTxtNombre().getText(), view.getTxtApellido().getText(),
				view.getTxtDni().getText(), view.getTxtTelefono().getText(), view.getTxtFechaNacimiento().getText(),
				(String) view.getCbTipo().getSelectedItem(), (String) view.getCbPosicion().getSelectedItem(),
				view.getTxtSalario().getText());
		model.altaEmpleado(empleado, LocalDate.now());
		JOptionPane.showMessageDialog(view.getFrame(), "Empleado " + empleado.getNombre() + " " + empleado.getApellido()
				+ " dado de alta correctamente", "Alta de empleado", JOptionPane.INFORMATION_MESSAGE);
		limpiarFormulario();
	}

	private void limpiarFormulario() {
		view.getTxtNombre().setText("");
		view.getTxtApellido().setText("");
		view.getTxtDni().setText("");
		view.getTxtTelefono().setText("");
		view.getTxtFechaNacimiento().setText("");
		view.getTxtSalario().setText("");
		view.getCbTipo().setSelectedIndex(0);
	}

	/**
	 * Validaciones sintacticas de los datos del formulario y conversion a la entidad.
	 * Es estatico y no usa la vista para poder probarlo de forma independiente.
	 */
	public static EmpleadoEntity crearEmpleado(String nombre, String apellido, String dni, String telefono,
			String fechaNacimiento, String tipoNombre, String posicionNombre, String salario) {
		EmpleadoEntity e = new EmpleadoEntity();
		e.setNombre(obligatorio(nombre, "nombre"));
		e.setApellido(obligatorio(apellido, "apellido"));
		e.setDni(validarDni(obligatorio(dni, "DNI")));
		e.setTelefono(validarTelefono(obligatorio(telefono, "teléfono")));
		e.setFechaNacimiento(validarFecha(obligatorio(fechaNacimiento, "fecha de nacimiento")));
		TipoEmpleado tipo = TipoEmpleado.fromNombre(tipoNombre);
		Posicion posicion = Posicion.fromNombre(posicionNombre);
		if (tipo == null || posicion == null)
			throw new ApplicationException("Hay que seleccionar el tipo de empleado y la posición");
		e.setTipo(tipo.name());
		e.setPosicion(posicion.name());
		e.setSalarioAnual(validarSalario(obligatorio(salario, "salario bruto anual")));
		return e;
	}

	private static String obligatorio(String valor, String campo) {
		if (valor == null || valor.trim().isEmpty())
			throw new ApplicationException("El campo " + campo + " es obligatorio");
		return valor.trim();
	}

	/** DNI: 8 cifras y la letra de control correspondiente */
	private static String validarDni(String dni) {
		String d = dni.toUpperCase();
		if (!d.matches("\\d{8}[A-Z]"))
			throw new ApplicationException("El DNI debe tener 8 cifras y una letra");
		int numero = Integer.parseInt(d.substring(0, 8));
		if (LETRAS_DNI.charAt(numero % 23) != d.charAt(8))
			throw new ApplicationException("La letra del DNI no es correcta");
		return d;
	}

	/** Telefono: 9 cifras (se admiten espacios entre ellas) */
	private static String validarTelefono(String telefono) {
		String t = telefono.replace(" ", "");
		if (!t.matches("\\d{9}"))
			throw new ApplicationException("El teléfono debe tener 9 cifras");
		return t;
	}

	/** Fecha en formato ISO aaaa-mm-dd y que exista en el calendario */
	private static String validarFecha(String fecha) {
		try {
			return LocalDate.parse(fecha).toString();
		} catch (DateTimeParseException ex) {
			throw new ApplicationException("La fecha de nacimiento debe ser una fecha válida con formato aaaa-mm-dd");
		}
	}

	/** Salario: numero con como mucho 2 decimales (se admite coma o punto decimal) */
	private static double validarSalario(String salario) {
		try {
			BigDecimal valor = new BigDecimal(salario.replace(',', '.'));
			if (valor.scale() > 2)
				throw new ApplicationException("El salario bruto anual admite como mucho 2 decimales");
			return valor.doubleValue();
		} catch (NumberFormatException ex) {
			throw new ApplicationException("El salario bruto anual debe ser un número");
		}
	}
}
