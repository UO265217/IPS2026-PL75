drop table if exists empleado;
create table empleado (
    id integer primary key autoincrement,
    nombre varchar(50) not null,
    apellido varchar(50) not null,
    dni varchar(9) not null unique,
    telefono varchar(15),
    fechaNacimiento date not null,
    tipo varchar(15) not null
                      check (tipo in ('DEPORTIVO', 'NO_DEPORTIVO')),
    posicion varchar(40) not null,
    salarioAnual decimal(10,2) not null
                      check (salarioAnual > 0)
)