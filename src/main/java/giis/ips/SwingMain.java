package giis.ips;

import giis.ips.util.Database;
import giis.ips.empleados.AltaEmpleadoController;
import giis.ips.empleados.AltaEmpleadoModel;
import giis.ips.empleados.AltaEmpleadoView;
import giis.ips.empleados.GestionEmpleadosController;
import giis.ips.empleados.GestionEmpleadosModel;
import giis.ips.empleados.GestionEmpleadosView;
import giis.ips.empleados.HorarioController;
import giis.ips.empleados.HorarioModel;
import giis.ips.empleados.HorarioSemanaController;
import giis.ips.empleados.HorarioSemanaView;
import giis.ips.empleados.HorarioView;
import giis.ips.entrenamientos.EntrenamientoController;
import giis.ips.entrenamientos.EntrenamientoModel;
import giis.ips.entrenamientos.EntrenamientoView;
import giis.ips.jardineria.JardineriaController;
import giis.ips.jardineria.JardineriaModel;
import giis.ips.jardineria.JardineriaView;
import giis.ips.reservas.ReservaController;
import giis.ips.reservas.ReservaModel;
import giis.ips.reservas.ReservaView;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Punto de entrada principal que incluye botones para la ejecucion de las pantallas 
 * de las aplicaciones de ejemplo
 * y acciones de inicializacion de la base de datos.
 * No sigue MVC pues es solamente temporal para que durante el desarrollo se tenga posibilidad
 * de realizar acciones de inicializacion
 */
public class SwingMain {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() { //NOSONAR codigo autogenerado
			public void run() {
				try {
					SwingMain window = new SwingMain();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace(); //NOSONAR codigo autogenerado
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public SwingMain() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Main");
		frame.setBounds(0, 0, 287, 340);
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

			
		JButton btnInicializarBaseDeDatos = new JButton("Inicializar Base de Datos en Blanco");
		btnInicializarBaseDeDatos.addActionListener(new ActionListener() { //NOSONAR codigo autogenerado
			public void actionPerformed(ActionEvent e) {
				Database db=new Database();
				db.createDatabase(false);
			}
		});
		frame.getContentPane().add(btnInicializarBaseDeDatos);
			
		JButton btnCargarDatosIniciales = new JButton("Cargar Datos Iniciales para Pruebas");
		btnCargarDatosIniciales.addActionListener(new ActionListener() { //NOSONAR codigo autogenerado
			public void actionPerformed(ActionEvent e) {
				Database db=new Database();
				db.createDatabase(false);
				db.loadDatabase();
			}
		});
		frame.getContentPane().add(btnCargarDatosIniciales);

		JButton btnInicializarAltaDeEmpleados = new JButton("Inicializar Alta de Empleados");
		btnInicializarAltaDeEmpleados.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AltaEmpleadoController controller = new AltaEmpleadoController(new AltaEmpleadoModel(), new AltaEmpleadoView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnInicializarAltaDeEmpleados);

		JButton btnGestionEmpleados = new JButton("Modificar y eliminar empleados");
		btnGestionEmpleados.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GestionEmpleadosController controller = new GestionEmpleadosController(new GestionEmpleadosModel(), new GestionEmpleadosView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnGestionEmpleados);

		JButton btnHorarios = new JButton("Horarios de empleados");
		btnHorarios.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HorarioController controller = new HorarioController(new HorarioModel(), new HorarioView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnHorarios);

		JButton btnHorarioSemana = new JButton("Ver mi horario semanal");
		btnHorarioSemana.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HorarioSemanaController controller = new HorarioSemanaController(new HorarioModel(), new HorarioSemanaView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnHorarioSemana);

		JButton btnReservas = new JButton("Reservar instalación (usuario externo)");
		btnReservas.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReservaController controller = new ReservaController(new ReservaModel(), new ReservaView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnReservas);

		JButton btnEntrenamientos = new JButton("Asignar entrenamiento a instalación");
		btnEntrenamientos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EntrenamientoController controller = new EntrenamientoController(new EntrenamientoModel(), new EntrenamientoView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnEntrenamientos);

		JButton btnJardineria = new JButton("Programar labor de jardinería");
		btnJardineria.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JardineriaController controller = new JardineriaController(new JardineriaModel(), new JardineriaView());
				controller.initController();
			}
		});
		frame.getContentPane().add(btnJardineria);
	}

	public JFrame getFrame() { return this.frame; }
	
}
