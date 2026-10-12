package giis.ips.empleados;

import lombok.Getter;
import lombok.Setter;

/**
 * Empleado del club (tabla empleado). Las fechas se guardan como String en formato ISO (aaaa-mm-dd);
 * tipo y posicion guardan el nombre de las constantes de TipoEmpleado y Posicion
 */
@Getter
@Setter
public class EmpleadoEntity {

    private int id;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String fechaNacimiento;
    private String tipo;
    private String posicion;
    private double salarioAnual;

}
