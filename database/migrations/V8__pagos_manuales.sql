-- V8: campos para registro MANUAL de pagos (transferencia / tarjeta) + soporte,
-- tanto en pagos de suscripción como en pagos de órdenes.
SET search_path TO gets_shared;

-- Pagos de suscripción
ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS metodo            varchar(50),
    ADD COLUMN IF NOT EXISTS banco             varchar(100),
    ADD COLUMN IF NOT EXISTS comprobante_url   varchar(500),
    ADD COLUMN IF NOT EXISTS ultimos_digitos   varchar(4),
    ADD COLUMN IF NOT EXISTS fecha_transaccion date;

-- Pagos de órdenes
ALTER TABLE pagos_orden
    ADD COLUMN IF NOT EXISTS banco             varchar(100),
    ADD COLUMN IF NOT EXISTS fecha_transaccion date,
    ADD COLUMN IF NOT EXISTS comprobante_url   varchar(500),
    ADD COLUMN IF NOT EXISTS ultimos_digitos   varchar(4);
