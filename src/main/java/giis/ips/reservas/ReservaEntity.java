package giis.ips.reservas;

import lombok.Getter;
import lombok.Setter;

/**
 * Reserva de una instalacion por un usuario externo (tabla reserva). La fecha se guarda como String
 * en formato ISO (aaaa-mm-dd) y las horas son minutos desde las 00:00
 */
@Getter
@Setter
public class ReservaEntity {

    private int id;
    private int instalacionId;
    private String fecha;
    private int horaInicio;
    private int horaFin;
    private String nombreUsuario;
    private String numeroTarjeta;
    private double importe;

}
