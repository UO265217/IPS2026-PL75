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
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Punto de entrada principal. Tras el acceso muestra el menu del rol (HU 37120): uno para el gerente, otro para el
 * empleado y otro para el usuario externo, cada uno solo con los accesos permitidos.
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

	/** Cabecera comun con el nombre del usuario (o "Invitado") y su rol, con el color propio de cada menu */
	private JPanel cabecera(Color fondo, String titulo) {
		JPanel cabecera = new JPanel(new MigLayout("insets 12 16 12 16, wrap 1", "[grow]"));
		cabecera.setBackground(fondo);
		JLabel lblTitulo = new JLabel(titulo);
		lblTitulo.setForeground(Color.WHITE);
		lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 16f));
		cabecera.add(lblTitulo);
		JLabel lblUsuario = new JLabel(MenuPrincipal.nombreMostrado(sesion) + " · " + MenuPrincipal.nombreRol(sesion.getRol()));
		lblUsuario.setName("lblUsuario");
		lblUsuario.setForeground(Color.WHITE);
		cabecera.add(lblUsuario);
		return cabecera;
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
		cuerpo.add(btnInicializarBaseDeDatos, "growx, span 2");

		JButton btnCargarDatosIniciales = new JButton("Cargar Datos Iniciales para Pruebas");
		btnCargarDatosIniciales.addActionListener(e -> {
			Database db = new Database();
			db.createDatabase(false);
			db.loadDatabase();
		});
		cuerpo.add(btnCargarDatosIniciales, "growx, span 2");
	}

	/** Menu del gerente: todas las funcionalidades en una cuadricula de dos columnas */
	private void menuGerente(JPanel cuerpo) {
		frame.setTitle("Panel de gestión");
		frame.getContentPane().add(cabecera(new Color(0x1F3A5F), "Panel del gerente"), BorderLayout.NORTH);
		cuerpo.setLayout(new MigLayout("insets 16, wrap 2", "[grow, fill][grow, fill]", "[]10[]"));
		List<Funcionalidad> opciones = MenuPrincipal.opciones(sesion.getRol());
		for (Funcionalidad f : opciones)
			cuerpo.add(botonDe(f), "h 48!");
		cuerpo.add(new JSeparator(), "span 2, growx, gaptop 10");
		anadirInicializacionBd(cuerpo);
		frame.setBounds(0, 0, 520, 480);
	}

	/** Menu del empleado: lista vertical con solo los accesos de su posicion, o un mensaje si no tiene ninguno */
	private void menuEmpleado(JPanel cuerpo) {
		frame.setTitle("Mi espacio");
		frame.getContentPane().add(cabecera(new Color(0x2E7D32), "Mi espacio de trabajo"), BorderLayout.NORTH);
		cuerpo.setLayout(new MigLayout("insets 16, wrap 1", "[grow, fill]", "[]8[]"));
		if (MenuPrincipal.sinOpciones(sesion.getRol())) {
			JLabel lblSinOpciones = new JLabel(MenuPrincipal.MENSAJE_SIN_OPCIONES);
			lblSinOpciones.setName("lblSinOpciones");
			cuerpo.add(lblSinOpciones, "align center");
		}
		for (Funcionalidad f : MenuPrincipal.opciones(sesion.getRol()))
			cuerpo.add(botonDe(f), "h 38!");
		frame.setBounds(0, 0, 360, 340);
	}

	/** Menu del usuario externo: pantalla sencilla centrada en reservar una instalacion */
	private void menuExterno(JPanel cuerpo) {
		frame.setTitle("Reserva de instalaciones");
		frame.getContentPane().add(cabecera(new Color(0xE65100), "Bienvenido al club"), BorderLayout.NORTH);
		cuerpo.setLayout(new MigLayout("insets 24, wrap 1, align center center", "[grow, fill]"));
		JLabel lblAyuda = new JLabel("¿Quieres usar una de nuestras instalaciones?");
		lblAyuda.setHorizontalAlignment(SwingConstants.CENTER);
		cuerpo.add(lblAyuda);
		JButton btnReservar = botonDe(Funcionalidad.RESERVAR_INSTALACION);
		btnReservar.setFont(btnReservar.getFont().deriveFont(Font.BOLD, 16f));
		cuerpo.add(btnReservar, "h 60!, gaptop 10");
		frame.setBounds(0, 0, 400, 280);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());

		JPanel cuerpo = new JPanel();
		switch (MenuPrincipal.tipoDe(sesion.getRol())) {
		case GERENTE:
			menuGerente(cuerpo);
			break;
		case EMPLEADO:
			menuEmpleado(cuerpo);
			break;
		default:
			menuExterno(cuerpo);
			break;
		}
		frame.getContentPane().add(cuerpo, BorderLayout.CENTER);

		JPanel pie = new JPanel(new MigLayout("insets 8 16 8 16", "[grow, right]"));
		pie.add(botonSalida());
		frame.getContentPane().add(pie, BorderLayout.SOUTH);
		frame.setLocationRelativeTo(null);
	}

	public JFrame getFrame() { return this.frame; }
	
}
