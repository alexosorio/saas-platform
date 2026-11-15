-- V5: código de barras del fabricante (EAN/UPC/GTIN) en productos.
SET search_path TO gets_shared;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS codigo_barras varchar(64);
CREATE INDEX IF NOT EXISTS idx_productos_codigo_barras ON productos (organizacion_id, codigo_barras);
