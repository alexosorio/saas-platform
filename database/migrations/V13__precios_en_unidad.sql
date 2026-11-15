-- V13: los precios pasan del PRODUCTO (entidad padre) a la UNIDAD.
--  - unidades.precio_venta nuevo; unidades.precio_compra ya existía.
--  - Se copia a cada unidad el precio que tenía su producto, para no alterar
--    los precios calculados de las listas existentes.
--  - productos.precio_venta / precio_compra se CONSERVAN (datos históricos) pero
--    la aplicación ya no los usa. Puedes eliminarlos más adelante (ver final).
SET search_path TO gets_shared;

ALTER TABLE unidades ADD COLUMN IF NOT EXISTS precio_venta NUMERIC(12,2);

UPDATE unidades u
   SET precio_venta = p.precio_venta
  FROM productos p
 WHERE u.producto_id = p.id
   AND u.precio_venta IS NULL
   AND p.precio_venta IS NOT NULL;

COMMENT ON COLUMN productos.precio_venta  IS 'OBSOLETO desde V13: el precio de venta vive en unidades.precio_venta';
COMMENT ON COLUMN productos.precio_compra IS 'OBSOLETO desde V13: el costo vive en unidades.precio_compra';

-- Opcional, cuando ya no necesites el dato histórico:
-- ALTER TABLE productos DROP COLUMN precio_venta, DROP COLUMN precio_compra;
