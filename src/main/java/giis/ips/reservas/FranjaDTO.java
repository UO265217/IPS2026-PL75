package giis.ips.reservas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Franja de un dia, con las horas en minutos desde las 00:00 (el inicio incluido y el fin no).
 * Se usa para mostrar las horas disponibles de una instalacion
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FranjaDTO {

    private int horaInicio;
    private int horaFin;

}
