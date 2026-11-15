-- Reconciliación entidad↔esquema (idempotente).
SET search_path TO gets_shared;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS updated_at                 timestamp;

ALTER TABLE roles_modulos
    ADD COLUMN IF NOT EXISTS created_at                 timestamp,
    ADD COLUMN IF NOT EXISTS modulo_codigo              varchar(255),
    ADD COLUMN IF NOT EXISTS modulo_nombre              varchar(255);
