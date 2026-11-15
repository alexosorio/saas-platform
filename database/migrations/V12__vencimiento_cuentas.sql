-- V12: fecha de vencimiento en cuentas por pagar/cobrar (base de la cartera vencida
--      a 30/60/90 días del dashboard). Rellena las existentes con creación + 30 días.
SET search_path TO gets_shared;

ALTER TABLE cuentas_por_pagar  ADD COLUMN IF NOT EXISTS fecha_vencimiento date;
ALTER TABLE cuentas_por_cobrar ADD COLUMN IF NOT EXISTS fecha_vencimiento date;

UPDATE cuentas_por_pagar  SET fecha_vencimiento = CAST(created_at AS date) + 30 WHERE fecha_vencimiento IS NULL;
UPDATE cuentas_por_cobrar SET fecha_vencimiento = CAST(created_at AS date) + 30 WHERE fecha_vencimiento IS NULL;

CREATE INDEX IF NOT EXISTS idx_cxp_vence ON cuentas_por_pagar(organizacion_id, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_cxc_vence ON cuentas_por_cobrar(organizacion_id, fecha_vencimiento);
