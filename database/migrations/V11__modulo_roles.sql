-- V11: módulo 'roles' para poder asignar la administración de roles de forma granular
--      (los endpoints de roles exigen permisos roles:leer/crear/actualizar/eliminar).
SET search_path TO gets_shared;

INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Roles y permisos', 'roles', 'Administración de roles y sus permisos por módulo', '/roles', 'user-shield', 3, TRUE)
ON CONFLICT (codigo) DO NOTHING;
