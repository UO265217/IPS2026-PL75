package giis.ips.login;

import lombok.Getter;

/**
 * Sesion abierta en la aplicacion (HU 37116): el rol de quien ha entrado y, si tiene cuenta, su nombre de
 * usuario y el empleado al que pertenece. El invitado no tiene nombre de usuario (null) ni empleado (0)
 */
@Getter
public class Sesion {

    private final Rol rol;
    private final String nombreUsuario;
    private final int empleadoId;

    public Sesion(Rol rol, String nombreUsuario, int empleadoId) {
        this.rol = rol;
        this.nombreUsuario = nombreUsuario;
        this.empleadoId = empleadoId;
    }

    public boolean esInvitado() {
        return rol.esInvitado();
    }
}
