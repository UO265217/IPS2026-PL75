package giis.ips.empleados;

import lombok.Getter;
import lombok.Setter;

/**
 * Turno del horario semanal de un empleado (tabla horario). El dia es 1 (lunes) ... 7 (domingo)
 * y las horas son minutos desde las 00:00.
 * Tambien representa los turnos de una semana concreta (HU 35205), donde especifico indica que el turno
 * viene de un horario de dia especifico en vez del periodico (no es una columna de la tabla)
 */
@Getter
@Setter
public class HorarioEntity {

    private int id;
    private int empleadoId;
    private int diaSemana;
    private int horaInicio;
    private int horaFin;
    private boolean especifico;

}
