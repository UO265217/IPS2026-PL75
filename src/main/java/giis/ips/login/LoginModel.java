package giis.ips.login;

import java.util.List;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Acceso a datos y reglas de negocio del login (HU 37116): inicio de sesion con una cuenta o entrada como invitado.
 * Si el usuario no existe o la contrasena no coincide se da el mismo error, para no revelar que cuentas existen.
 */
public class LoginModel {
	static final String ERROR_CREDENCIALES = "Usuario o contraseña incorrectos";

	private Database db = new Database();

	/** Comprueba usuario y contrasena y devuelve la sesion con el rol de la cuenta */
	public Sesion iniciarSesion(String nombreUsuario, String contrasena) {
		validateCondition(nombreUsuario != null && !nombreUsuario.trim().isEmpty() && contrasena != null
				&& !contrasena.trim().isEmpty(), "Introduce el usuario y la contraseña");
		String usuario = nombreUsuario.trim();

		String sql = "select u.empleadoId, e.posicion from usuario u join empleado e on e.id = u.empleadoId"
				+ " where u.nombreUsuario = ? and u.contrasena = ?";
		List<Object[]> filas = db.executeQueryArray(sql, usuario, Contrasena.hash(contrasena));
		validateCondition(!filas.isEmpty(), ERROR_CREDENCIALES);

		Rol rol = Rol.deEmpleado((String) filas.get(0)[1]);
		validateCondition(rol != null, "La cuenta no tiene un rol válido");
		return new Sesion(rol, usuario, ((Number) filas.get(0)[0]).intValue());
	}

	/** El invitado entra sin cuenta, con el rol de invitado */
	public Sesion entrarComoInvitado() {
		return new Sesion(Rol.INVITADO, null, 0);
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
