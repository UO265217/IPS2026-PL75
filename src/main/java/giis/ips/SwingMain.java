package giis.ips;

import giis.ips.util.Database;
import giis.ips.util.SwingUtil;
import giis.ips.login.Funcionalidad;
import giis.ips.login.LoginController;
import giis.ips.login.LoginModel;
import giis.ips.login.LoginView;
import giis.ips.login.MenuPrincipal;
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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Punto de entrada principal. Tras el acceso muestra la lista de opciones que puede hacer el rol (HU 37120).
 * No sigue MVC pues es solamente temporal para que durante el desarrollo se tenga posibilidad
 * de realizar acciones de inicializacion de la base de datos
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

	/** Accion que abre la pantalla de cada funcionalidad */
	private Runnable accionDe(Funcionalidad funcionalidad) {
		switch (funcionalidad) {
		case ALTA_EMPLEADOS:
			return () -> new AltaEmpleadoController(new AltaEmpleadoModel(), new AltaEmpleadoView()).initController();
		case GESTION_EMPLEADOS:
			return () -> new GestionEmpleadosController(new GestionEmpleadosModel(), new GestionEmpleadosView())
					.initController();
		case HORARIOS_EMPLEADOS:
			return () -> new HorarioController(new HorarioModel(), new HorarioView()).initController();
		case MI_HORARIO_SEMANAL:
			return () -> new HorarioSemanaController(new HorarioModel(), new HorarioSemanaView()).initController();
		case RESERVAR_INSTALACION:
			return () -> new ReservaController(new ReservaModel(), new ReservaView()).initController();
		case ASIGNAR_ENTRENAMIENTO:
			return () -> new EntrenamientoController(new EntrenamientoModel(), new EntrenamientoView())
					.initController();
		case PROGRAMAR_JARDINERIA:
			return () -> new JardineriaController(new JardineriaModel(), new JardineriaView()).initController();
		default:
			throw new IllegalArgumentException("Funcionalidad sin pantalla: " + funcionalidad);
		}
	}

	/**
	 * Boton de una funcionalidad. Antes de ejecutar su accion se comprueba el rol de la sesion (HU 37116):
	 * si no tiene acceso no se hace nada y se muestra un aviso
	 */
	private JButton botonDe(Funcionalidad funcionalidad) {
		Runnable accion = accionDe(funcionalidad);
		JButton boton = new JButton(funcionalidad.getNombre());
		boton.setName("btn" + funcionalidad.name());
		boton.addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			Permisos.exigir(sesion.getRol(), funcionalidad);
			accion.run();
		}));
		return boton;
	}

	/** Linea con el nombre del usuario (o "Invitado") y su rol */
	private JLabel etiquetaSesion() {
		JLabel lblUsuario = new JLabel(" Sesión: " + MenuPrincipal.nombreMostrado(sesion) + " (" + MenuPrincipal.nombreRol(sesion.getRol()) + ")");
		lblUsuario.setName("lblUsuario");
		return lblUsuario;
	}

	/** Cerrar la sesion (o salir del modo invitado) vuelve a la pantalla de acceso */
	private JButton botonSalida() {
		JButton btnCerrarSesion = new JButton(MenuPrincipal.textoSalida(sesion));
		btnCerrarSesion.setName("btnCerrarSesion");
		btnCerrarSesion.addActionListener(e -> {
			mostrarLogin(); // primero se abre el acceso para que nunca falte una ventana abierta
			frame.dispose();
		});
		return btnCerrarSesion;
	}

	/** Opciones de arranque de la base de datos, solo para el gerente mientras no haya otra forma de cargar datos */
	private void anadirInicializacionBd(JPanel cuerpo) {
		JButton btnInicializarBaseDeDatos = new JButton("Inicializar Base de Datos en Blanco");
		btnInicializarBaseDeDatos.addActionListener(e -> new Database().createDatabase(false));
		cuerpo.add(btnInicializarBaseDeDatos, "growx");

		JButton btnCargarDatosIniciales = new JButton("Cargar Datos Iniciales para Pruebas");
		btnCargarDatosIniciales.addActionListener(e -> {
			Database db = new Database();
			db.createDatabase(false);
			db.loadDatabase();
		});
		cuerpo.add(btnCargarDatosIniciales, "growx");
	}

	/**
	 * Initialize the contents of the frame. El menu es una lista con solo las opciones que el rol puede hacer (HU 37120)
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Main");
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(etiquetaSesion(), BorderLayout.NORTH);

		JPanel cuerpo = new JPanel(new MigLayout("insets 8, wrap 1", "[grow, fill]"));
		if (MenuPrincipal.tipoDe(sesion.getRol()) == MenuPrincipal.Tipo.GERENTE)
			anadirInicializacionBd(cuerpo);
		if (MenuPrincipal.sinOpciones(sesion.getRol())) {
			JLabel lblSinOpciones = new JLabel(MenuPrincipal.MENSAJE_SIN_OPCIONES);
			lblSinOpciones.setName("lblSinOpciones");
			cuerpo.add(lblSinOpciones);
		}
		for (Funcionalidad f : MenuPrincipal.opciones(sesion.getRol()))
			cuerpo.add(botonDe(f));
		cuerpo.add(botonSalida());
		frame.getContentPane().add(cuerpo, BorderLayout.CENTER);

		frame.pack();
		frame.setMinimumSize(new java.awt.Dimension(300, 0));
		frame.setLocationRelativeTo(null);
	}

	public JFrame getFrame() { return this.frame; }
	
}
