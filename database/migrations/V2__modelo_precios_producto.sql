-- ═══════════════════════════════════════════════════════════════════════
--  MIGRACION V2 — Modelo de precios centrado en el producto
--  + listas de precios como FORMULAS + auditoria de cambios + proveedores
--  Schema: gets_shared   |   PostgreSQL
-- ═══════════════════════════════════════════════════════════════════════
SET search_path TO gets_shared;

-- ─────────────────────────────────────────────────────────────────────
-- 1. PROVEEDORES (tabla nueva)
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS proveedores (
    id              BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    nombre          VARCHAR(200) NOT NULL,
    identificacion  VARCHAR(50),            -- NIT / RUT
    telefono        VARCHAR(50),
    email           VARCHAR(150),
    direccion       VARCHAR(300),
    contacto        VARCHAR(150),           -- persona de contacto
    notas           TEXT,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_proveedores_org ON proveedores(organizacion_id);

-- ─────────────────────────────────────────────────────────────────────
-- 2. PRODUCTOS: precio_venta, precio_compra, proveedor_id, tipo, requiere_imei
-- ─────────────────────────────────────────────────────────────────────
ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS precio_venta   NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS precio_compra  NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS proveedor_id   BIGINT,
    ADD COLUMN IF NOT EXISTS tipo_producto  VARCHAR(50) DEFAULT 'accesorio',
    ADD COLUMN IF NOT EXISTS requiere_imei  BOOLEAN NOT NULL DEFAULT FALSE;

-- requiere_imei se deriva de tipo_producto: 'movil' => TRUE.
-- Lo guardamos explicito para que el formulario de unidades lo lea directo.

DO $$ BEGIN
    ALTER TABLE productos
        ADD CONSTRAINT fk_productos_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ─────────────────────────────────────────────────────────────────────
-- 3. AUDITORIA DE CAMBIOS DE PRECIO (tabla nueva)
--    Una fila por cada cambio de precio_venta y/o precio_compra.
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS historial_precios_producto (
    id                     BIGSERIAL PRIMARY KEY,
    producto_id            BIGINT NOT NULL,
    precio_venta_anterior  NUMERIC(15,2),
    precio_venta_nuevo     NUMERIC(15,2),
    precio_compra_anterior NUMERIC(15,2),
    precio_compra_nuevo    NUMERIC(15,2),
    usuario_id             BIGINT,          -- quien hizo el cambio (del JWT)
    motivo                 VARCHAR(300),
    fecha_cambio           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_histprecio_producto FOREIGN KEY (producto_id)
        REFERENCES productos(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_histprecio_producto ON historial_precios_producto(producto_id);
CREATE INDEX IF NOT EXISTS idx_histprecio_fecha    ON historial_precios_producto(fecha_cambio);

-- ─────────────────────────────────────────────────────────────────────
-- 4. LISTAS_PRECIOS: ahora son FORMULAS
--    base_calculo: sobre que precio del producto se calcula
--    tipo_ajuste:  margen porcentual o valor fijo
--    valor_ajuste: el numero (porcentaje o valor)
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    CREATE TYPE base_calculo_lista AS ENUM ('precio_venta', 'precio_compra');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE tipo_ajuste_lista AS ENUM ('margen_porcentaje', 'valor_fijo');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE listas_precios
    ADD COLUMN IF NOT EXISTS base_calculo base_calculo_lista NOT NULL DEFAULT 'precio_venta',
    ADD COLUMN IF NOT EXISTS tipo_ajuste  tipo_ajuste_lista  NOT NULL DEFAULT 'margen_porcentaje',
    ADD COLUMN IF NOT EXISTS valor_ajuste NUMERIC(15,4)      NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────────────────────────────────
-- 5. PRECIOS (tabla existente) -> se reutiliza como OVERRIDE opcional.
--    Semantica nueva: si hay fila en `precios` para (lista, producto),
--    ese valor MANDA sobre la formula. Si no hay, se usa la formula.
--    No requiere cambios estructurales.
--    Si quieres modelo 100% formula, puedes vaciarla:  TRUNCATE precios;
-- ─────────────────────────────────────────────────────────────────────

-- ═══════════════════════════════════════════════════════════════════════
--  REGLA DE CALCULO DEL PRECIO DE UN PRODUCTO EN UNA LISTA
--  ───────────────────────────────────────────────────────────────────
--  precio(lista, producto) =
--     SI existe override en `precios`  -> ese valor
--     SINO:
--       base = (lista.base_calculo = 'precio_venta')
--                 ? producto.precio_venta
--                 : producto.precio_compra
--       SI lista.tipo_ajuste = 'margen_porcentaje'
--                 -> base * (1 + lista.valor_ajuste/100)
--       SI lista.tipo_ajuste = 'valor_fijo'
--                 -> lista.valor_ajuste
-- ═══════════════════════════════════════════════════════════════════════
