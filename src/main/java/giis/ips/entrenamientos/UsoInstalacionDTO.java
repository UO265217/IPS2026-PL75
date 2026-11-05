package giis.ips.entrenamientos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cualquier cosa que ocupa una instalacion en un dia: un entrenamiento, un partido, una reserva de un usuario externo
 * o una labor de jardineria. El tipo es una de las constantes de esta clase; el id es el de la fila en su tabla y la
 * descripcion, lo que se muestra (el equipo, el usuario externo o la labor). Horas en minutos desde las 00:00.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsoInstalacionDTO {
    public static final String ENTRENAMIENTO = "ENTRENAMIENTO";
    public static final String PARTIDO = "PARTIDO";
    public static final String RESERVA = "RESERVA";
    public static final String JARDINERIA = "JARDINERIA";

    private String tipo;
    private int id;
    private String descripcion;
    private int horaInicio;
    private int horaFin;

    /** Uso de un equipo de la entidad: no se puede asignar un entrenamiento que coincida con el */
    public boolean esDeEquipo() {
        return ENTRENAMIENTO.equals(tipo) || PARTIDO.equals(tipo);
    }

    /** Texto para los mensajes, por ejemplo "el entrenamiento del Juvenil A de 10:00 a 12:00" */
    public String describir() {
        String que;
        switch (tipo) {
        case ENTRENAMIENTO:
            que = "el entrenamiento del " + descripcion;
            break;
        case PARTIDO:
            que = "el partido del " + descripcion;
            break;
        case RESERVA:
            que = "la reserva externa de " + descripcion;
            break;
        default:
            que = "la labor de jardinería «" + descripcion + "»";
        }
        return que + " de " + EntrenamientoModel.formatearHora(horaInicio) + " a "
                + EntrenamientoModel.formatearHora(horaFin);
    }

}
