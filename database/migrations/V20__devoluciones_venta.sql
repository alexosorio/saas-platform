-- V20: devoluciones de venta (cualquier motivo) sobre órdenes ya confirmadas.
--  - Entrada al inventario (o revisión / baja según el estado de la unidad devuelta)
--  - Reduce lo que el cliente debe de la orden; el excedente pagado -> nota crédito al cliente
--    (reembolso o saldo a favor utilizable como forma de pago en compras futuras)
SET search_path TO gets_shared;

ALTER TABLE ordenes ADD COLUMN IF NOT EXISTS monto_devuelto NUMERIC(14,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS devoluciones_venta (
    id                    BIGSERIAL PRIMARY KEY,
    organizacion_id       BIGINT NOT NULL,
    orden_id              BIGINT NOT NULL REFERENCES ordenes(id),
    cliente_id            BIGINT REFERENCES clientes(id),
    numero                VARCHAR(40) NOT NULL,
    tipo_motivo           VARCHAR(30) NOT NULL,      -- garantia | cambio | error_venta | desistimiento | otro
    observacion           TEXT NOT NULL,
    monto_total           NUMERIC(14,2) NOT NULL,    -- valor acreditado por lo devuelto
    monto_aplicado_saldo  NUMERIC(14,2) NOT NULL DEFAULT 0,  -- parte que redujo lo que el cliente debía
    monto_nota_credito    NUMERIC(14,2) NOT NULL DEFAULT 0,  -- excedente ya pagado -> nota crédito al cliente
    tratamiento           VARCHAR(20),               -- reembolso | saldo_a_favor (si hubo excedente)
    metodo_reembolso      VARCHAR(30),
    referencia_reembolso  VARCHAR(100),
    usuario_id            BIGINT,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_devoluciones_orden ON devoluciones_venta (orden_id);

CREATE TABLE IF NOT EXISTS detalles_devolucion (
    id               BIGSERIAL PRIMARY KEY,
    devolucion_id    BIGINT NOT NULL REFERENCES devoluciones_venta(id),
    detalle_orden_id BIGINT NOT NULL REFERENCES detalles_orden(id),
    unidad_id        BIGINT NOT NULL REFERENCES unidades(id),
    cantidad         INTEGER NOT NULL,
    monto            NUMERIC(14,2) NOT NULL,
    destino          VARCHAR(20) NOT NULL,           -- disponible | revision | baja
    bodega_id        BIGINT REFERENCES bodegas(id)
);
CREATE INDEX IF NOT EXISTS idx_det_devolucion_detalle ON detalles_devolucion (detalle_orden_id);

CREATE TABLE IF NOT EXISTS notas_credito_cliente (
    id               BIGSERIAL PRIMARY KEY,
    organizacion_id  BIGINT NOT NULL,
    cliente_id       BIGINT REFERENCES clientes(id),
    devolucion_id    BIGINT REFERENCES devoluciones_venta(id),
    monto            NUMERIC(14,2) NOT NULL,
    saldo_disponible NUMERIC(14,2) NOT NULL DEFAULT 0,
    tratamiento      VARCHAR(20) NOT NULL,           -- reembolso | saldo_a_favor
    estado           VARCHAR(20) NOT NULL,           -- reembolsada | disponible | aplicada
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_nc_cliente ON notas_credito_cliente (cliente_id, estado);

-- Nueva forma de pago: usar el saldo a favor del cliente
ALTER TYPE metodo_pago_orden ADD VALUE IF NOT EXISTS 'nota_credito';

-- Permiso para registrar devoluciones (mueven dinero: reembolsos y saldos a favor)
INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Devoluciones de venta', 'devoluciones', 'Registrar devoluciones de órdenes confirmadas (reembolsos y saldos a favor)', '/catalog/orders', 'undo', 21, TRUE)
ON CONFLICT (codigo) DO NOTHING;
