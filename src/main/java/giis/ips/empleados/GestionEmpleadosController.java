package giis.ips.empleados;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import giis.ips.util.ApplicationException;
import giis.ips.util.SwingUtil;

/**
 * Controlador de la modificacion y eliminacion de empleados: instala los manejadores de la vista,
 * realiza las validaciones sintacticas y llama al modelo.
 */
public class GestionEmpleadosController {
	private static final String[] COLUMNAS = { "Id", "Apellido", "Nombre", "DNI", "Posición" };

	private GestionEmpleadosModel model;
	private GestionEmpleadosView view;
	private EmpleadoEntity seleccionado; // empleado que se esta editando o null si no hay ninguno

	public GestionEmpleadosController(GestionEmpleadosModel m, GestionEmpleadosView v) {
		this.model = m;
		this.view = v;
		this.initView();
	}

	/** Inicializacion del controlador: anyade los manejadores de eventos */
	public void initController() {
		// la seleccion de una fila no se puede hacer con una lambda (MouseListener tiene varios metodos)
		view.getTabEmpleados().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				SwingUtil.exceptionWrapper(() -> seleccionarEmpleado());
			}
		});
		view.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> guardar()));
		view.getBtnEliminar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> eliminar()));
		view.getBtnCerrar().addActionListener(e -> view.getFrame().dispose());
	}

	public void initView() {
		cargarEmpleados();
		mostrarEmpleado(null);
		view.getFrame().setVisible(true);
	}

	/** Carga la tabla de empleados; la primera columna es el id (como String) para usarlo de clave */
	public void cargarEmpleados() {
		List<EmpleadoEntity> empleados = model.getEmpleados();
		DefaultTableModel tm = new DefaultTableModel(COLUMNAS, 0);
		for (EmpleadoEntity e : empleados)
			tm.addRow(new Object[] { String.valueOf(e.getId()), e.getApellido(), e.getNombre(), e.getDni(),
					Posicion.valueOf(e.getPosicion()).getNombre() });
		view.getTabEmpleados().setModel(tm);
		SwingUtil.autoAdjustColumns(view.getTabEmpleados());
	}

	public void seleccionarEmpleado() {
		String key = SwingUtil.getSelectedKey(view.getTabEmpleados());
		mostrarEmpleado(key.isEmpty() ? null : model.getEmpleado(Integer.parseInt(key)));
	}

	/** Rellena el formulario con el empleado (o lo vacia si es null) y activa los botones solo si hay uno */
	private void mostrarEmpleado(EmpleadoEntity e) {
		seleccionado = e;
		boolean hay = e != null;
		view.getTxtNombre().setText(hay ? e.getNombre() : "");
		view.getTxtApellido().setText(hay ? e.getApellido() : "");
		view.getTxtDni().setText(hay ? e.getDni() : "");
		view.getTxtTelefono().setText(hay ? e.getTelefono() : "");
		view.getTxtFechaNacimiento().setText(hay ? e.getFechaNacimiento() : "");
		view.getTxtSalario().setText(hay ? formatearSalario(e.getSalarioAnual()) : "");
		view.getTxtTipo().setText(hay ? TipoEmpleado.valueOf(e.getTipo()).getNombre() : "");
		view.getTxtPosicion().setText(hay ? Posicion.valueOf(e.getPosicion()).getNombre() : "");
		view.getBtnGuardar().setEnabled(hay);
		view.getBtnEliminar().setEnabled(hay);
	}

	public void guardar() {
		if (seleccionado == null)
			throw new ApplicationException("Hay que seleccionar un empleado");
		EmpleadoEntity empleado = crearEmpleadoModificado(seleccionado, view.getTxtNombre().getText(),
				view.getTxtApellido().getText(), view.getTxtDni().getText(), view.getTxtTelefono().getText(),
				view.getTxtFechaNacimiento().getText(), view.getTxtSalario().getText());
		model.modificarEmpleado(empleado, LocalDate.now());
		JOptionPane.showMessageDialog(view.getFrame(), "Empleado " + empleado.getNombre() + " "
				+ empleado.getApellido() + " modificado correctamente", "Modificar empleado",
				JOptionPane.INFORMATION_MESSAGE);
		cargarEmpleados();
		SwingUtil.selectAndGetSelectedKey(view.getTabEmpleados(), String.valueOf(empleado.getId()));
		mostrarEmpleado(model.getEmpleado(empleado.getId()));
	}

	public void eliminar() {
		if (seleccionado == null)
			throw new ApplicationException("Hay que seleccionar un empleado");
		String nombre = seleccionado.getNombre() + " " + seleccionado.getApellido();
		int respuesta = JOptionPane.showConfirmDialog(view.getFrame(),
				"¿Seguro que quieres eliminar del sistema a " + nombre + "?", "Eliminar empleado",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (respuesta != JOptionPane.YES_OPTION)
			return;
		model.eliminarEmpleado(seleccionado.getId());
		JOptionPane.showMessageDialog(view.getFrame(), "Empleado " + nombre + " eliminado del sistema",
				"Eliminar empleado", JOptionPane.INFORMATION_MESSAGE);
		cargarEmpleados();
		mostrarEmpleado(null);
	}

	/**
	 * Validaciones sintacticas de los datos modificados y conversion a la entidad.
	 * Reutiliza las del alta; el tipo y la posicion se toman del empleado original porque no se pueden cambiar.
	 * Es estatico y no usa la vista para poder probarlo de forma independiente.
	 */
	public static EmpleadoEntity crearEmpleadoModificado(EmpleadoEntity original, String nombre, String apellido,
			String dni, String telefono, String fechaNacimiento, String salario) {
		EmpleadoEntity e = AltaEmpleadoController.crearEmpleado(nombre, apellido, dni, telefono, fechaNacimiento,
				TipoEmpleado.valueOf(original.getTipo()).getNombre(), Posicion.valueOf(original.getPosicion()).getNombre(),
				salario);
		e.setId(original.getId());
		return e;
	}

	/** Salario sin ceros decimales sobrantes (60000.0 -> 60000, 15000.50 -> 15000.5) */
	private static String formatearSalario(double salario) {
		return BigDecimal.valueOf(salario).stripTrailingZeros().toPlainString();
	}
}
