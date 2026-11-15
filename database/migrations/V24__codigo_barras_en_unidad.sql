-- V24: el código de barras pertenece a la UNIDAD, no al producto general
-- Cada unidad trae el suyo en el empaque. Si es el EAN del modelo, varias unidades pueden compartirlo;
-- al escanearlo en una venta se toma una unidad disponible (y se pregunta el color si hay varios).
SET search_path TO gets_shared;

ALTER TABLE unidades ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_unidades_codigo_barras ON unidades (codigo_barras) WHERE codigo_barras IS NOT NULL;

-- Las unidades existentes heredan el código que tenía su producto (si lo tenía)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema = 'gets_shared' AND table_name = 'productos' AND column_name = 'codigo_barras') THEN
    UPDATE unidades u
       SET codigo_barras = p.codigo_barras
      FROM productos p
     WHERE u.producto_id = p.id
       AND u.codigo_barras IS NULL
       AND p.codigo_barras IS NOT NULL AND btrim(p.codigo_barras) <> '';
  END IF;
END $$;

-- El producto general ya no lleva código de barras
DROP INDEX IF EXISTS idx_productos_codigo_barras;
ALTER TABLE productos DROP COLUMN IF EXISTS codigo_barras;
