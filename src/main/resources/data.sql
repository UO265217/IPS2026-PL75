--Datos para carga inicial de la base de datos (pruebas)
delete from empleado;
insert into empleado(nombre, apellido, dni, telefono, fechaNacimiento, tipo, posicion, salarioAnual) values
    ('Ana', 'García', '11111111H', '600111111', '1980-05-20', 'NO_DEPORTIVO', 'GERENTE', 60000),
    ('Luis', 'Pérez', '22222222J', '600222222', '1995-03-14', 'DEPORTIVO', 'JUGADOR', 120000),
    ('Marta', 'López', '33333333P', '600333333', '1978-11-02', 'DEPORTIVO', 'ENTRENADOR', 80000),
    ('Hugo', 'Fernández', '44444444A', '600444444', '2010-07-30', 'DEPORTIVO', 'JUGADOR', 15000.50);
