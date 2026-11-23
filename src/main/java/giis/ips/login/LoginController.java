package giis.ips.login;

import java.util.function.Consumer;

import giis.ips.util.SwingUtil;

/**
 * Controlador del inicio de sesion (HU 37116): llama al modelo con lo escrito en la vista y,
 * si la entrada es correcta, cierra la pantalla y entrega la sesion a quien la haya pedido
 * (en la aplicacion, al menu principal). Si no, el modelo lanza el error y se muestra al usuario
 */
public class LoginController {
	private LoginModel model;
	private LoginView view;
	private Consumer<Sesion> alEntrar;

	public LoginController(LoginModel m, LoginView v, Consumer<Sesion> alEntrar) {
		this.model = m;
		this.view = v;
		this.alEntrar = alEntrar;
		this.initView();
	}

	public void initController() {
		view.getBtnEntrar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> entrar()));
		view.getBtnInvitado().addActionListener(e -> SwingUtil.exceptionWrapper(() -> entrarComoInvitado()));
	}

	public void initView() {
		view.getFrame().setVisible(true);
	}

	public void entrar() {
		Sesion sesion = model.iniciarSesion(view.getTxtUsuario().getText(),
				new String(view.getTxtContrasena().getPassword()));
		abrir(sesion);
	}

	public void entrarComoInvitado() {
		abrir(model.entrarComoInvitado());
	}

	private void abrir(Sesion sesion) {
		view.getFrame().dispose();
		alEntrar.accept(sesion);
	}
}
