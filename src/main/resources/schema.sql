drop table if exists reserva;
drop table if exists partido;
drop table if exists entrenamiento;
drop table if exists equipo;
drop table if exists instalacion;
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

create table instalacion (
    id integer primary key autoincrement,
    nombre varchar(50) not null unique
);

create table equipo (
    id integer primary key autoincrement,
    nombre varchar(50) not null unique
);

--Usos de las instalaciones por los equipos de la entidad (HU 35206 y 36124). Tras acabar cualquiera
--de ellos no se admiten reservas de usuarios externos en esa instalacion hasta pasada hora y media
create table entrenamiento (
    id integer primary key autoincrement,
    equipoId integer not null references equipo(id),
    instalacionId integer not null references instalacion(id),
    fecha date not null,
    horaInicio integer not null,
    horaFin integer not null,
    check (horaInicio >= 0 and horaFin <= 1440 and horaInicio < horaFin)
);

create table partido (
    id integer primary key autoincrement,
    equipoId integer not null references equipo(id),
    instalacionId integer not null references instalacion(id),
    fecha date not null,
    horaInicio integer not null,
    horaFin integer not null,
    check (horaInicio >= 0 and horaFin <= 1440 and horaInicio < horaFin)
);

--Reservas de usuarios externos (HU 35206): entre las 8:00 y las 22:00, de al menos 1 hora, a 50 euros la hora
create table reserva (
    id integer primary key autoincrement,
    instalacionId integer not null references instalacion(id),
    fecha date not null,
    horaInicio integer not null,
    horaFin integer not null,
    nombreUsuario varchar(100) not null,
    numeroTarjeta varchar(19) not null,
    importe decimal(10,2) not null
                      check (importe > 0),
    check (horaInicio >= 480 and horaFin <= 1320 and horaFin - horaInicio >= 60)
);
