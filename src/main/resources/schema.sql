drop table if exists horario_especifico;
drop table if exists horario;
drop table if exists empleado;
create table empleado (
    id integer primary key autoincrement,
    nombre varchar(50) not null,
    apellido varchar(50) not null,
    dni varchar(9) not null unique,
    telefono varchar(15) not null,
    fechaNacimiento date not null,
    tipo varchar(15) not null
                      check (tipo in ('DEPORTIVO', 'NO_DEPORTIVO')),
    posicion varchar(40) not null,
    salarioAnual decimal(10,2) not null
                      check (salarioAnual > 0)
);

--Turnos del horario semanal por defecto de los empleados no deportivos.
--Las horas son minutos desde las 00:00 (0..1440); diaSemana: 1=lunes ... 7=domingo
create table horario (
    id integer primary key autoincrement,
    empleadoId integer not null references empleado(id),
    diaSemana integer not null check (diaSemana between 1 and 7),
    horaInicio integer not null,
    horaFin integer not null,
    check (horaInicio >= 0 and horaFin <= 1440 and horaInicio < horaFin)
);

--Turnos de un horario de dia especifico (HU 35204): sobrescriben en su totalidad el horario periodico de esa fecha.
create table horario_especifico (
    id integer primary key autoincrement,
    empleadoId integer not null references empleado(id),
    fecha date not null,
    horaInicio integer not null,
    horaFin integer not null,
    check (horaInicio >= 0 and horaFin <= 1440 and horaInicio < horaFin)
);
