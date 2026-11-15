-- V21: módulo de devoluciones con asiento contable (partida doble).
--  - asientos_contables / movimientos_contables: inmutables (solo inserción)
--  - Cuentas del PUC configurables por organización (Configuración → Contabilidad)
--    Formato del valor: "CÓDIGO Nombre de la cuenta". VALÍDALAS CON TU CONTADOR.
SET search_path TO gets_shared;

CREATE TABLE IF NOT EXISTS asientos_contables (
    id              BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo            VARCHAR(40) NOT NULL,          -- devolucion_venta, ...
    referencia      VARCHAR(60) NOT NULL,          -- p. ej. número de la devolución
    documento_id    BIGINT,
    descripcion     TEXT,
    total_debe      NUMERIC(16,2) NOT NULL,
    total_haber     NUMERIC(16,2) NOT NULL,
    usuario_id      BIGINT,
    CONSTRAINT asiento_cuadrado CHECK (total_debe = total_haber)
);
CREATE INDEX IF NOT EXISTS idx_asientos_org_fecha ON asientos_contables (organizacion_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_asientos_doc ON asientos_contables (tipo, documento_id);

CREATE TABLE IF NOT EXISTS movimientos_contables (
    id            BIGSERIAL PRIMARY KEY,
    asiento_id    BIGINT NOT NULL REFERENCES asientos_contables(id),
    cuenta_codigo VARCHAR(20)  NOT NULL,
    cuenta_nombre VARCHAR(150),
    debe          NUMERIC(16,2) NOT NULL DEFAULT 0,
    haber         NUMERIC(16,2) NOT NULL DEFAULT 0,
    tercero       VARCHAR(200),
    detalle       VARCHAR(300),
    CONSTRAINT movimiento_un_lado CHECK ((debe > 0 AND haber = 0) OR (haber > 0 AND debe = 0))
);
CREATE INDEX IF NOT EXISTS idx_movcont_asiento ON movimientos_contables (asiento_id);

-- Inmutables: un asiento registrado no se modifica ni se borra (se corrige con otro asiento)
CREATE OR REPLACE FUNCTION contabilidad_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Los asientos contables no se modifican ni eliminan: registra un asiento de corrección';
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_asientos_inmutables ON asientos_contables;
CREATE TRIGGER trg_asientos_inmutables BEFORE UPDATE OR DELETE ON asientos_contables
    FOR EACH ROW EXECUTE FUNCTION contabilidad_inmutable();
DROP TRIGGER IF EXISTS trg_movcont_inmutables ON movimientos_contables;
CREATE TRIGGER trg_movcont_inmutables BEFORE UPDATE OR DELETE ON movimientos_contables
    FOR EACH ROW EXECUTE FUNCTION contabilidad_inmutable();

-- Cuentas configurables (PUC – Decreto 2650 de 1993; ajústalas a tu plan de cuentas)
INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion) VALUES
 ('contab.devoluciones_ventas',  '4175 Devoluciones en ventas',                  'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: devoluciones en ventas',  'Débito por el valor neto devuelto (sin IVA).'),
 ('contab.iva_por_pagar',        '2408 Impuesto sobre las ventas por pagar',     'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: IVA por pagar',          'Débito por el IVA de lo devuelto.'),
 ('contab.clientes',             '1305 Clientes',                                'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: clientes',               'Crédito por lo que el cliente dejó de deber.'),
 ('contab.caja',                 '1105 Caja',                                    'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: caja',                   'Crédito por reembolsos en efectivo.'),
 ('contab.bancos',               '1110 Bancos',                                  'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: bancos',                 'Crédito por reembolsos por transferencia o tarjeta.'),
 ('contab.anticipos_clientes',   '2805 Anticipos y avances recibidos',           'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: saldo a favor de clientes', 'Crédito cuando el excedente queda como saldo a favor.'),
 ('contab.inventario',           '1435 Mercancías no fabricadas por la empresa', 'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: inventario',             'Débito por el costo de las unidades que reingresan.'),
 ('contab.costo_ventas',         '6135 Comercio al por mayor y al por menor',    'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: costo de ventas',        'Crédito por el costo de las unidades devueltas.'),
 ('contab.perdida_inventario',   '5315 Gastos extraordinarios',                  'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: pérdida de inventario',  'Débito por el costo de unidades devueltas inservibles.')
ON CONFLICT DO NOTHING;
