package giis.ips.empleados;

import lombok.Getter;
import lombok.Setter;

/**
 * Turno de un horario de dia especifico de un empleado (tabla horario_especifico). La fecha se guarda como String
 * en formato ISO (aaaa-mm-dd) y las horas son minutos desde las 00:00
 */
@Getter
@Setter
public class HorarioEspecificoEntity {

    private int id;
    private int empleadoId;
    private String fecha;
    private int horaInicio;
    private int horaFin;

}
