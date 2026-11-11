package giis.ips.jardineria;

import lombok.Getter;
import lombok.Setter;

/**
 * Labor de jardineria programada en una instalacion (tabla labor_jardineria). La fecha se guarda como String
 * en formato ISO (aaaa-mm-dd) y las horas son minutos desde las 00:00
 */
@Getter
@Setter
public class LaborJardineriaEntity {

    private int id;
    private int instalacionId;
    private int empleadoId;
    private String descripcion;
    private String fecha;
    private int horaInicio;
    private int horaFin;

}
