-- ============================================================================
-- V6: columnas faltantes en 'usuarios' y 'roles' que exigen las entidades
-- del user-service (verificación, recuperación, bloqueo, organización, rol).
--
-- IMPORTANTE: NO fuerza el esquema. Ejecútalo conectado (o con el search_path)
-- apuntando al esquema donde REALMENTE están tus tablas. Para saberlo:
--   SELECT table_schema, table_name FROM information_schema.tables
--   WHERE table_name IN ('usuarios','roles');
--
-- Si están en public:        SET search_path TO public;
-- Si están en gets_shared:   SET search_path TO gets_shared;
-- (descomenta la línea correcta o define tu search_path antes de correrlo)
-- ============================================================================
-- SET search_path TO public;

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS codigo_verificacion        varchar(255),
    ADD COLUMN IF NOT EXISTS codigo_verificacion_expira timestamp,
    ADD COLUMN IF NOT EXISTS token_recuperacion         varchar(255),
    ADD COLUMN IF NOT EXISTS token_recuperacion_expira  timestamp,
    ADD COLUMN IF NOT EXISTS intentos_fallidos          integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bloqueado_hasta            timestamp,
    ADD COLUMN IF NOT EXISTS ultimo_cambio_password     timestamp,
    ADD COLUMN IF NOT EXISTS organizacion_id            bigint,
    ADD COLUMN IF NOT EXISTS rol_id                     bigint;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS es_sistema boolean DEFAULT FALSE NOT NULL;

-- FK usuarios.rol_id -> roles(id) (si ambas tablas están en el mismo esquema y aún no existe)
DO $$ BEGIN
    ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles(id);
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_table THEN NULL; END $$;

CREATE INDEX IF NOT EXISTS idx_usuarios_organizacion ON usuarios(organizacion_id);
