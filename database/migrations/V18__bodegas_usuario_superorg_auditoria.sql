-- V18: venta por bodega, súper organización y auditoría.
-- IMPORTANTE: ejecutar ANTES de desplegar los servicios nuevos (la auditoría se escribe
-- en la misma transacción de cada operación crítica).
SET search_path TO gets_shared;

-- ── 1. Bodegas asignadas a cada usuario ─────────────────────────────────
CREATE TABLE IF NOT EXISTS usuarios_bodegas (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    bodega_id  BIGINT NOT NULL REFERENCES bodegas(id)  ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, bodega_id)
);

INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo) VALUES
 ('Vender desde otras bodegas', 'ventas_otra_bodega',
  'Permite incluir en órdenes unidades de bodegas distintas a las asignadas al usuario', '/catalog/orders', 'warehouse', 20, TRUE),
 ('Auditoría', 'auditoria', 'Consulta del registro de auditoría de operaciones críticas', '/auditoria', 'history', 5, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ── 2. Súper organización (dueña de la plataforma) ──────────────────────
ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS es_plataforma BOOLEAN NOT NULL DEFAULT FALSE;
-- Solo puede existir una
CREATE UNIQUE INDEX IF NOT EXISTS ux_organizacion_plataforma ON organizaciones (es_plataforma) WHERE es_plataforma;

INSERT INTO organizaciones (nombre, slug, email, activo, es_plataforma)
SELECT 'Administración de la plataforma', 'plataforma', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM organizaciones WHERE es_plataforma);

-- Los usuarios con rol Superadministrador pasan a la súper organización
UPDATE usuarios SET organizacion_id = (SELECT id FROM organizaciones WHERE es_plataforma)
 WHERE rol_id = (SELECT id FROM roles WHERE nombre = 'Superadministrador' AND organizacion_id IS NULL);

-- ── 3. Auditoría de operaciones críticas (solo inserción) ────────────────
CREATE TABLE IF NOT EXISTS auditoria (
    id              BIGSERIAL PRIMARY KEY,
    fecha           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    organizacion_id BIGINT,
    usuario_id      BIGINT,
    usuario_email   VARCHAR(255),
    servicio        VARCHAR(40)  NOT NULL,
    accion          VARCHAR(60)  NOT NULL,
    entidad         VARCHAR(60)  NOT NULL,
    entidad_id      VARCHAR(60),
    detalle         TEXT,
    ip              VARCHAR(64),
    user_agent      VARCHAR(300)
);
CREATE INDEX IF NOT EXISTS idx_auditoria_org_fecha ON auditoria (organizacion_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_entidad   ON auditoria (entidad, entidad_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_accion    ON auditoria (accion);

-- Inmutable: ni la aplicación ni un acceso directo pueden modificar o borrar registros
CREATE OR REPLACE FUNCTION auditoria_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La auditoría es de solo inserción: no se puede modificar ni eliminar';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_inmutable ON auditoria;
CREATE TRIGGER trg_auditoria_inmutable BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_inmutable();
