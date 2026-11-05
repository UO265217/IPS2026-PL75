package giis.ips.entrenamientos;

import lombok.Getter;
import lombok.Setter;

/**
 * Entrenamiento de un equipo en una instalacion (tabla entrenamiento). La fecha se guarda como String
 * en formato ISO (aaaa-mm-dd) y las horas son minutos desde las 00:00
 */
@Getter
@Setter
public class EntrenamientoEntity {

    private int id;
    private int equipoId;
    private int instalacionId;
    private String fecha;
    private int horaInicio;
    private int horaFin;

}
