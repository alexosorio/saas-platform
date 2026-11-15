-- V19: las unidades no se eliminan, se DAN DE BAJA con motivo.
--  - Movimiento de salida en inventario
--  - Nota crédito sobre la cuenta por pagar (error de registro / devolución al proveedor)
--  - Registro de auditoría (desde la aplicación)
SET search_path TO gets_shared;

ALTER TYPE estado_unidad ADD VALUE IF NOT EXISTS 'dado_de_baja';

ALTER TABLE unidades
    ADD COLUMN IF NOT EXISTS tipo_baja    VARCHAR(30),
    ADD COLUMN IF NOT EXISTS motivo_baja  TEXT,
    ADD COLUMN IF NOT EXISTS fecha_baja   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_baja BIGINT;

-- Notas sobre cuentas por pagar (crédito = reduce lo que se le debe al proveedor)
CREATE TABLE IF NOT EXISTS notas_cuenta_pagar (
    id                  BIGSERIAL PRIMARY KEY,
    cuenta_por_pagar_id BIGINT NOT NULL REFERENCES cuentas_por_pagar(id),
    unidad_id           BIGINT REFERENCES unidades(id),
    tipo                VARCHAR(20)  NOT NULL DEFAULT 'credito',
    monto               NUMERIC(14,2) NOT NULL,
    motivo              TEXT NOT NULL,
    usuario_id          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notas_cxp_cuenta ON notas_cuenta_pagar (cuenta_por_pagar_id);
