package giis.ips.login;

import lombok.Getter;
import lombok.Setter;

/**
 * Cuenta de acceso de un empleado (tabla usuario, HU 37116)
 */
@Getter
@Setter
public class UsuarioEntity {

    private int id;
    private String nombreUsuario;
    private String contrasena;
    private int empleadoId;

}
