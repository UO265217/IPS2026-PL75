package giis.ips.jardineria;

import giis.ips.entrenamientos.EntrenamientoModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Labor ya programada de un jardinero en un dia, en cualquier instalacion, con el nombre de la instalacion para
 * mostrarla. Horas en minutos desde las 00:00
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaborJardineroDTO {

    private int id;
    private int instalacionId;
    private String instalacion;
    private String descripcion;
    private int horaInicio;
    private int horaFin;

    /** Texto para los mensajes, por ejemplo "de 09:00 a 10:00 (Abonar el césped, Estadio)" */
    public String describir() {
        return "de " + EntrenamientoModel.formatearHora(horaInicio) + " a " + EntrenamientoModel.formatearHora(horaFin)
                + " (" + descripcion + ", " + instalacion + ")";
    }

}
