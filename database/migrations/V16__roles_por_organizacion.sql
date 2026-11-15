-- V16: roles aislados por organización.
--   organizacion_id NULL  = rol del SISTEMA (global; solo la plataforma lo modifica)
--   organizacion_id = N   = rol propio de la organización N (solo ella lo ve y lo edita)
-- Ejecutar DESPUÉS de V15 (V15 usa la restricción única por nombre que aquí se reemplaza).
SET search_path TO gets_shared;

ALTER TABLE roles ADD COLUMN IF NOT EXISTS organizacion_id bigint;

-- 1) Los roles base son del sistema
UPDATE roles SET es_sistema = TRUE
 WHERE organizacion_id IS NULL
   AND nombre IN ('Propietario', 'Administrador', 'Editor', 'Miembro', 'Invitado', 'Superadministrador');

-- 2) Roles personalizados existentes: se asignan a la organización de sus usuarios cuando no hay ambigüedad
UPDATE roles r
   SET organizacion_id = x.org
  FROM (SELECT rol_id, MIN(organizacion_id) AS org
          FROM usuarios
         WHERE rol_id IS NOT NULL AND organizacion_id IS NOT NULL
         GROUP BY rol_id
        HAVING COUNT(DISTINCT organizacion_id) = 1) x
 WHERE r.id = x.rol_id
   AND r.organizacion_id IS NULL
   AND COALESCE(r.es_sistema, FALSE) = FALSE;

-- 3) Nombre único dentro de cada organización (antes: único en toda la plataforma)
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_nombre_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_org_nombre ON roles (COALESCE(organizacion_id, 0), nombre);
CREATE INDEX IF NOT EXISTS idx_roles_organizacion ON roles (organizacion_id);

-- Revisión: roles que quedaron globales sin ser del sistema (sin usuarios, o usados por varias
-- organizaciones). Decide si asignarles una organización o eliminarlos.
SELECT id, nombre, organizacion_id, es_sistema
  FROM roles
 WHERE organizacion_id IS NULL AND COALESCE(es_sistema, FALSE) = FALSE;
