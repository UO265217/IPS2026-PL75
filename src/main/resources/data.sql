--Datos para carga inicial de la base de datos (pruebas)
delete from horario;
delete from empleado;
insert into empleado(nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual) values
    ('Ana', 'García', '11111111H', '600111111', '1980-05-20', 'NO_DEPORTIVO', 'GERENTE', 60000),
    ('Luis', 'Pérez', '22222222J', '600222222', '1995-03-14', 'DEPORTIVO', 'JUGADOR', 120000),
    ('Marta', 'López', '33333333P', '600333333', '1978-11-02', 'DEPORTIVO', 'ENTRENADOR', 80000),
    ('Hugo', 'Fernández', '44444444A', '600444444', '2010-07-30', 'DEPORTIVO', 'JUGADOR', 15000.50),
    ('Carlos', 'Ruiz', '66666666Z', '600666666', '1988-02-10', 'NO_DEPORTIVO', 'VENDEDOR_ENTRADAS', 24000),
    ('Lucía', 'Gómez', '77777777B', '600777777', '1992-09-25', 'NO_DEPORTIVO', 'ENCARGADO_TIENDA', 28000),
    ('Pedro', 'Sanz', '88888888Y', '600888888', '1975-12-01', 'NO_DEPORTIVO', 'JARDINERIA', 21000),
    ('Sara', 'Vidal', '99999999R', '600999999', '1985-06-18', 'NO_DEPORTIVO', 'DIRECTOR_COMUNICACIONES', 45000);

--Horarios de ejemplo (minutos desde las 00:00; dia 1=lunes ... 7=domingo)
--Ana (gerente): lunes a viernes de 9:00 a 14:00 y los lunes tambien de 16:00 a 18:00 (27 h)
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 540, 840 from empleado, (select 1 as dia union select 2 union select 3 union select 4 union select 5)
    where dni = '11111111H';
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, 1, 960, 1080 from empleado where dni = '11111111H';

--Carlos (vendedor de entradas): turno partido de lunes a viernes, 9:00-13:00 y 16:00-20:00 (40 h, el limite)
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 540, 780 from empleado, (select 1 as dia union select 2 union select 3 union select 4 union select 5)
    where dni = '66666666Z';
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 960, 1200 from empleado, (select 1 as dia union select 2 union select 3 union select 4 union select 5)
    where dni = '66666666Z';

--Lucia (encargada de tienda): jueves a sabado 10:00-14:00 y 17:00-21:00, domingo 10:00-14:00 (28 h)
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 600, 840 from empleado, (select 4 as dia union select 5 union select 6 union select 7)
    where dni = '77777777B';
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 1020, 1260 from empleado, (select 4 as dia union select 5 union select 6)
    where dni = '77777777B';

--Pedro (jardineria): de lunes a sabado, de 7:00 a 13:00 (36 h)
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 420, 780 from empleado,
        (select 1 as dia union select 2 union select 3 union select 4 union select 5 union select 6)
    where dni = '88888888Y';

--Sara (director de comunicaciones): sin horario, para ver una lista vacia
