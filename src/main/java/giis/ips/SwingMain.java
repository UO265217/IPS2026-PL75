package giis.ips;

import giis.ips.util.Database;
import giis.ips.util.SwingUtil;
import giis.ips.login.Funcionalidad;
import giis.ips.login.LoginController;
import giis.ips.login.LoginModel;
import giis.ips.login.LoginView;
import giis.ips.login.Permisos;
import giis.ips.login.Sesion;
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
import javax.swing.JLabel;
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
	private final Sesion sesion;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() { //NOSONAR codigo autogenerado
			public void run() {
				try {
					mostrarLogin(); // primero el inicio de sesion (HU 37116)
				} catch (Exception e) {
					e.printStackTrace(); //NOSONAR codigo autogenerado
				}
			}
		});
	}

	/** Pantalla de acceso; al entrar (con cuenta o como invitado) se abre el menu con la sesion obtenida */
	private static void mostrarLogin() {
		new LoginController(new LoginModel(), new LoginView(), sesion -> {
			SwingMain window = new SwingMain(sesion);
			window.frame.setVisible(true);
		}).initController();
	}

	/**
	 * Create the application.
	 */
	public SwingMain(Sesion sesion) {
		this.sesion = sesion;
		initialize();
	}

	/**
	 * Anyade la opcion al menu. Antes de ejecutar su accion se comprueba el rol de la sesion (HU 37116):
	 * si no tiene acceso no se hace nada y se muestra un aviso
	 */
	private void anadirOpcion(Funcionalidad funcionalidad, JButton boton) {
		ActionListener[] acciones = boton.getActionListeners();
		for (ActionListener a : acciones)
			boton.removeActionListener(a);
		boton.addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			Permisos.exigir(sesion.getRol(), funcionalidad);
			for (ActionListener a : acciones)
				a.actionPerformed(e);
		}));
		frame.getContentPane().add(boton);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Main");
		frame.setBounds(0, 0, 287, 390);
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		String quien = sesion.esInvitado() ? "Invitado" : sesion.getNombreUsuario();
		frame.getContentPane().add(new JLabel(" Sesión: " + quien + " (" + sesion.getRol() + ")"));

			
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
		anadirOpcion(Funcionalidad.ALTA_EMPLEADOS, btnInicializarAltaDeEmpleados);

		JButton btnGestionEmpleados = new JButton("Modificar y eliminar empleados");
		btnGestionEmpleados.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GestionEmpleadosController controller = new GestionEmpleadosController(new GestionEmpleadosModel(), new GestionEmpleadosView());
				controller.initController();
			}
		});
		anadirOpcion(Funcionalidad.GESTION_EMPLEADOS, btnGestionEmpleados);

		JButton btnHorarios = new JButton("Horarios de empleados");
		btnHorarios.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HorarioController controller = new HorarioController(new HorarioModel(), new HorarioView());
				controller.initController();
			}
		});
		anadirOpcion(Funcionalidad.HORARIOS_EMPLEADOS, btnHorarios);

		JButton btnHorarioSemana = new JButton("Ver mi horario semanal");
		btnHorarioSemana.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HorarioSemanaController controller = new HorarioSemanaController(new HorarioModel(), new HorarioSemanaView());
				controller.initController();
			}
		});
		anadirOpcion(Funcionalidad.MI_HORARIO_SEMANAL, btnHorarioSemana);

		JButton btnReservas = new JButton("Reservar instalación (usuario externo)");
		btnReservas.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReservaController controller = new ReservaController(new ReservaModel(), new ReservaView());
				controller.initController();
			}
		});
		anadirOpcion(Funcionalidad.RESERVAR_INSTALACION, btnReservas);

		JButton btnEntrenamientos = new JButton("Asignar entrenamiento a instalación");
		btnEntrenamientos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EntrenamientoController controller = new EntrenamientoController(new EntrenamientoModel(), new EntrenamientoView());
				controller.initController();
			}
		});
		anadirOpcion(Funcionalidad.ASIGNAR_ENTRENAMIENTO, btnEntrenamientos);

		JButton btnJardineria = new JButton("Programar labor de jardinería");
		btnJardineria.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JardineriaController controller = new JardineriaController(new JardineriaModel(), new JardineriaView());
				controller.initController();
			}
		});
		anadirOpcion(Funcionalidad.PROGRAMAR_JARDINERIA, btnJardineria);

		// Cerrar la sesion (o salir del modo invitado) vuelve a la pantalla de acceso
		JButton btnCerrarSesion = new JButton(sesion.esInvitado() ? "Salir del modo invitado" : "Cerrar sesión");
		btnCerrarSesion.setName("btnCerrarSesion");
		btnCerrarSesion.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mostrarLogin(); // primero se abre el acceso para que nunca falte una ventana abierta
				frame.dispose();
			}
		});
		frame.getContentPane().add(btnCerrarSesion);
	}

	public JFrame getFrame() { return this.frame; }
	
}
