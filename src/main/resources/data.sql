--Datos para carga inicial de la base de datos (pruebas)
delete from horario;
delete from empleado;
insert into empleado(nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual) values
    ('Ana', 'García', '11111111H', '600111111', '1980-05-20', 'NO_DEPORTIVO', 'GERENTE', 60000),
    ('Luis', 'Pérez', '22222222J', '600222222', '1995-03-14', 'DEPORTIVO', 'JUGADOR', 120000),
    ('Marta', 'López', '33333333P', '600333333', '1978-11-02', 'DEPORTIVO', 'ENTRENADOR', 80000),
    ('Hugo', 'Fernández', '44444444A', '600444444', '2010-07-30', 'DEPORTIVO', 'JUGADOR', 15000.50);

--Horario de Ana (gerente): lunes a viernes de 9:00 a 14:00 y los lunes tambien de 16:00 a 18:00
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, dia, 540, 840 from empleado, (select 1 as dia union select 2 union select 3 union select 4 union select 5)
    where dni = '11111111H';
insert into horario(empleadoId, diaSemana, horaInicio, horaFin)
    select id, 1, 960, 1080 from empleado where dni = '11111111H';
