-- V10: cuentas por pagar / por cobrar + pagos a proveedor
SET search_path TO gets_shared;

-- Cuentas por pagar (una por unidad ingresada, por su precio de compra)
CREATE TABLE IF NOT EXISTS cuentas_por_pagar (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    unidad_id BIGINT REFERENCES unidades(id),
    proveedor_id BIGINT REFERENCES proveedores(id),
    monto NUMERIC(14,2) NOT NULL,
    saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cxp_org ON cuentas_por_pagar(organizacion_id);
CREATE INDEX IF NOT EXISTS idx_cxp_estado ON cuentas_por_pagar(organizacion_id, estado);

-- Pagos a proveedor (con soporte de la transferencia)
CREATE TABLE IF NOT EXISTS pagos_proveedor (
    id BIGSERIAL PRIMARY KEY,
    cuenta_por_pagar_id BIGINT NOT NULL REFERENCES cuentas_por_pagar(id) ON DELETE CASCADE,
    monto NUMERIC(14,2) NOT NULL,
    metodo VARCHAR(50),
    banco VARCHAR(100),
    comprobante_url VARCHAR(500),
    ultimos_digitos VARCHAR(4),
    fecha_transaccion DATE,
    referencia VARCHAR(255),
    observaciones TEXT,
    usuario_registro BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pagoprov_cuenta ON pagos_proveedor(cuenta_por_pagar_id);

-- Cuentas por cobrar (una por orden con saldo pendiente)
CREATE TABLE IF NOT EXISTS cuentas_por_cobrar (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    orden_id BIGINT REFERENCES ordenes(id),
    cliente_id BIGINT REFERENCES clientes(id),
    monto NUMERIC(14,2) NOT NULL,
    saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cxc_org ON cuentas_por_cobrar(organizacion_id);
CREATE INDEX IF NOT EXISTS idx_cxc_estado ON cuentas_por_cobrar(organizacion_id, estado);

