-- V4: stock reservado (bloqueo por pago en proceso). Esquema del catálogo.
SET search_path TO gets_shared;
ALTER TABLE inventario ADD COLUMN IF NOT EXISTS cantidad_reservada integer NOT NULL DEFAULT 0;
