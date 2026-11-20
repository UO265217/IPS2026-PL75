package giis.ips.login;

import java.util.List;

import giis.ips.util.ApplicationException;
import giis.ips.util.Database;

/**
 * Cuentas de acceso de los empleados (HU 37116): se definen al dar de alta al empleado (HU 35201) y el gerente
 * puede cambiarlas. La contrasena se guarda siempre como hash. Un empleado tiene como mucho una cuenta
 */
public class CuentaModel {
	public static final int MAX_USUARIO = 30;

	private Database db = new Database();

	/** Nombre de usuario de la cuenta del empleado o null si no tiene */
	public String getNombreUsuario(int empleadoId) {
		List<Object[]> filas = db.executeQueryArray("select nombreUsuario from usuario where empleadoId = ?", empleadoId);
		return filas.isEmpty() ? null : (String) filas.get(0)[0];
	}

	/** Crea la cuenta de un empleado que aun no tiene */
	public void crear(int empleadoId, String nombreUsuario, String contrasena) {
		validateCondition(getNombreUsuario(empleadoId) == null, "El empleado ya tiene una cuenta");
		validarDatos(nombreUsuario, contrasena);
		validateCondition(!existeUsuarioDeOtroEmpleado(nombreUsuario, empleadoId),
				"Ya existe una cuenta con el usuario " + nombreUsuario);
		db.executeUpdate("insert into usuario(nombreUsuario, contrasena, empleadoId) values (?, ?, ?)", nombreUsuario,
				Contrasena.hash(contrasena), empleadoId);
	}

	/**
	 * Cambia la cuenta de un empleado. Si el usuario viene vacio es porque no se quiere cuenta, y solo vale si
	 * el empleado no la tenia. Con la contrasena vacia se conserva la que hubiera; para una cuenta nueva es obligatoria
	 */
	public void modificar(int empleadoId, String nombreUsuario, String contrasenaNueva) {
		String actual = getNombreUsuario(empleadoId);
		boolean sinUsuario = nombreUsuario == null || nombreUsuario.trim().isEmpty();
		boolean sinContrasena = contrasenaNueva == null || contrasenaNueva.isEmpty();
		if (sinUsuario) {
			validateCondition(actual == null, "El usuario es obligatorio");
			validateCondition(sinContrasena, "Indica también el usuario de la cuenta");
			return;
		}
		if (actual == null) {
			validateCondition(!sinContrasena, "La contraseña es obligatoria para crear la cuenta");
			crear(empleadoId, nombreUsuario, contrasenaNueva);
			return;
		}
		validarDatos(nombreUsuario, sinContrasena ? "x" : contrasenaNueva);
		validateCondition(!existeUsuarioDeOtroEmpleado(nombreUsuario, empleadoId),
				"Ya existe una cuenta con el usuario " + nombreUsuario);
		db.executeUpdate("update usuario set nombreUsuario = ? where empleadoId = ?", nombreUsuario, empleadoId);
		if (!sinContrasena)
			db.executeUpdate("update usuario set contrasena = ? where empleadoId = ?", Contrasena.hash(contrasenaNueva),
					empleadoId);
	}

	/** Borra la cuenta del empleado, si la tiene */
	public void eliminar(int empleadoId) {
		db.executeUpdate("delete from usuario where empleadoId = ?", empleadoId);
	}

	/** El usuario es unico: no puede tenerlo la cuenta de ningun otro empleado */
	public boolean existeUsuarioDeOtroEmpleado(String nombreUsuario, int empleadoId) {
		return !db.executeQueryArray("select id from usuario where nombreUsuario = ? and empleadoId <> ?",
				nombreUsuario, empleadoId).isEmpty();
	}

	private void validarDatos(String nombreUsuario, String contrasena) {
		validateCondition(nombreUsuario != null && !nombreUsuario.trim().isEmpty(), "El usuario es obligatorio");
		validateCondition(nombreUsuario.length() <= MAX_USUARIO,
				"El usuario admite como mucho " + MAX_USUARIO + " caracteres");
		validateCondition(contrasena != null && !contrasena.isEmpty(), "La contraseña es obligatoria");
	}

	private void validateCondition(boolean condition, String message) {
		if (!condition)
			throw new ApplicationException(message);
	}
}
