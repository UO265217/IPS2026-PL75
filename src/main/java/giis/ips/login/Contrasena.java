package giis.ips.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import giis.ips.util.UnexpectedException;

/**
 * Hash de las contrasenas (HU 37116): en la base de datos se guarda el SHA-256 en hexadecimal (64 caracteres),
 * nunca la contrasena en claro
 */
public final class Contrasena {

	private Contrasena() {
	}

	public static String hash(String contrasena) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(contrasena.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new UnexpectedException(e);
		}
	}
}
