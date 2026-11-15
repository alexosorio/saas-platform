-- V14: completa código y nombre de módulo en los permisos de roles.
-- Los permisos sembrados por el esquema base quedaron sin modulo_codigo/modulo_nombre:
-- la pantalla no mostraba el nombre y la autorización los ignoraba.
SET search_path TO gets_shared;

UPDATE roles_modulos rm
   SET modulo_codigo = m.codigo,
       modulo_nombre = m.nombre
  FROM modulos m
 WHERE rm.modulo_id = m.id
   AND (rm.modulo_codigo IS NULL OR rm.modulo_nombre IS NULL
        OR rm.modulo_codigo <> m.codigo OR rm.modulo_nombre <> m.nombre);

-- Verificación: debe devolver 0
SELECT COUNT(*) AS permisos_sin_codigo FROM roles_modulos WHERE modulo_codigo IS NULL;
