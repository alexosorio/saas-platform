-- ============================================================================
-- V3: Gating de pago (suscripción pendiente hasta pagar) + Habeas Data en pagos
-- Esquema de subscription-service (tablas en 'public').
-- ============================================================================
SET search_path TO public;

-- 1) Nuevo estado para suscripciones creadas sin pago confirmado.
ALTER TYPE estado_suscripcion ADD VALUE IF NOT EXISTS 'pendiente_pago';

-- 2) Registro de autorización de tratamiento de datos (Ley 1581/2012 - Habeas Data)
ALTER TABLE metodos_pago
    ADD COLUMN IF NOT EXISTS autorizacion_datos boolean,
    ADD COLUMN IF NOT EXISTS fecha_autorizacion timestamp,
    ADD COLUMN IF NOT EXISTS version_politica varchar(20);
