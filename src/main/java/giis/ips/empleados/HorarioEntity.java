package giis.ips.empleados;

import lombok.Getter;
import lombok.Setter;

/**
 * Turno del horario semanal de un empleado (tabla horario). El dia es 1 (lunes) ... 7 (domingo)
 * y las horas son minutos desde las 00:00
 */
@Getter
@Setter
public class HorarioEntity {

    private int id;
    private int empleadoId;
    private int diaSemana;
    private int horaInicio;
    private int horaFin;

}
