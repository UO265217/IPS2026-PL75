package giis.ips.jardineria;

import lombok.Getter;
import lombok.Setter;

/** Empleado de jardineria que se puede elegir para hacer una labor */
@Getter
@Setter
public class JardineroDTO {

    private int id;
    private String nombre;
    private String apellido;

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

}
