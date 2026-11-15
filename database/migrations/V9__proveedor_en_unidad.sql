-- V9: el proveedor pasa del PRODUCTO a la UNIDAD (distintas unidades del mismo
-- producto pueden venir de proveedores diferentes).
SET search_path TO gets_shared;

-- 1) Agregar proveedor_id a unidades
ALTER TABLE unidades ADD COLUMN IF NOT EXISTS proveedor_id bigint;
DO $$ BEGIN
    ALTER TABLE unidades ADD CONSTRAINT fk_unidades_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id);
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_table THEN NULL; END $$;
CREATE INDEX IF NOT EXISTS idx_unidades_proveedor ON unidades(proveedor_id);

-- 2) (Opcional) migrar datos: copiar el proveedor del producto a sus unidades antes de borrarlo
UPDATE unidades u SET proveedor_id = p.proveedor_id
FROM productos p
WHERE u.producto_id = p.id AND p.proveedor_id IS NOT NULL AND u.proveedor_id IS NULL;

-- 3) Quitar proveedor_id de productos
ALTER TABLE productos DROP COLUMN IF EXISTS proveedor_id;
