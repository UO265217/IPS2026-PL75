package giis.ips.login.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import giis.ips.login.Contrasena;

/**
 * Pruebas del hash de contrasenas de la HU 37116: SHA-256 en hexadecimal (64 caracteres, lo que cabe en la columna)
 */
class TestContrasena {

	@Test
	void vectorConocidoDeSha256() {
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Contrasena.hash("abc"));
	}

	@Test
	void mismaContrasenaMismoHash() {
		assertEquals(Contrasena.hash("ana123"), Contrasena.hash("ana123"));
		assertEquals(64, Contrasena.hash("ana123").length());
	}

	@Test
	void contrasenasDistintasHashDistinto() {
		assertNotEquals(Contrasena.hash("ana123"), Contrasena.hash("Ana123"));
	}

	@Test
	void tildesYAcentosSeCodificanEnUtf8() {
		assertEquals(Contrasena.hash("contraseña"), Contrasena.hash("contraseña"));
		assertNotEquals(Contrasena.hash("contrasena"), Contrasena.hash("contraseña"));
	}
}
