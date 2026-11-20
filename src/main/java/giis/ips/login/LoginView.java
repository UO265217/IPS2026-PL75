package giis.ips.login;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de inicio de sesion (HU 37116): usuario y contrasena, o entrar como invitado
 */
@Getter
public class LoginView {

	private JFrame frame;

	private JTextField txtUsuario;
	private JPasswordField txtContrasena;
	private JButton btnEntrar;
	private JButton btnInvitado;

	public LoginView() {
		initialize();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Iniciar sesión");
		frame.setName("Login");
		frame.setBounds(0, 0, 340, 210);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", ""));

		frame.getContentPane().add(new JLabel("Usuario:"));
		txtUsuario = new JTextField(15);
		txtUsuario.setName("txtUsuario");
		frame.getContentPane().add(txtUsuario, "growx, wrap");

		frame.getContentPane().add(new JLabel("Contraseña:"));
		txtContrasena = new JPasswordField(15);
		txtContrasena.setName("txtContrasena");
		frame.getContentPane().add(txtContrasena, "growx, wrap");

		btnInvitado = new JButton("Entrar como invitado");
		btnInvitado.setName("btnInvitado");
		frame.getContentPane().add(btnInvitado, "span 2, split 2, right, gaptop 15");

		btnEntrar = new JButton("Entrar");
		btnEntrar.setName("btnEntrar");
		frame.getContentPane().add(btnEntrar);
		frame.getRootPane().setDefaultButton(btnEntrar); // Enter inicia sesion
	}
}
