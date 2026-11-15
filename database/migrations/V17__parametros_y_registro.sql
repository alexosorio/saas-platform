-- V17: módulo de parámetros configurables (reemplaza valores fijos del application.yml)
--   organizacion_id NULL -> valor general (por defecto, lo define la plataforma)
--   organizacion_id = N  -> valor propio de la organización N (reemplaza al general)
--   alcance 'plataforma'   -> solo lo modifica el administrador de la plataforma
--   alcance 'organizacion' -> cada organización puede ajustarlo
SET search_path TO gets_shared;

CREATE TABLE IF NOT EXISTS parametros (
    id              BIGSERIAL PRIMARY KEY,
    clave           VARCHAR(80)  NOT NULL,
    organizacion_id BIGINT,
    valor           VARCHAR(500) NOT NULL,
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'entero',
    alcance         VARCHAR(20)  NOT NULL DEFAULT 'organizacion',
    categoria       VARCHAR(50),
    nombre          VARCHAR(120),
    descripcion     TEXT,
    minimo          NUMERIC,
    maximo          NUMERIC,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_parametros_clave_org ON parametros (clave, COALESCE(organizacion_id, 0));

INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion, minimo, maximo) VALUES
 ('suscripcion.dias_prueba',     '15', 'entero', 'plataforma',   'Suscripción', 'Días de prueba gratuita',
  'Duración de la prueba gratuita al registrarse una organización nueva.', 0, 90),
 ('suscripcion.dias_gracia',     '3',  'entero', 'plataforma',   'Suscripción', 'Días de gracia por no pago',
  'Días después del vencimiento antes de suspender automáticamente una suscripción sin pago.', 0, 60),
 ('sesion.minutos_inactividad',  '30', 'entero', 'organizacion', 'Seguridad',   'Cierre de sesión por inactividad (minutos)',
  'Minutos sin actividad tras los cuales se cierra la sesión automáticamente.', 5, 480),
 ('cuentas.plazo_pagar_dias',    '30', 'entero', 'organizacion', 'Finanzas',    'Plazo de cuentas por pagar (días)',
  'Días para el vencimiento de cada cuenta por pagar desde que ingresa la unidad.', 0, 365),
 ('cuentas.plazo_cobrar_dias',   '30', 'entero', 'organizacion', 'Finanzas',    'Plazo de cuentas por cobrar (días)',
  'Días para el vencimiento de cada cuenta por cobrar desde que se confirma la orden.', 0, 365),
 ('inventario.umbral_stock_bajo','5',  'entero', 'organizacion', 'Inventario',  'Umbral de stock bajo',
  'Cantidad disponible a partir de la cual un producto se considera con stock bajo en el tablero.', 0, 1000)
ON CONFLICT DO NOTHING;

INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Configuración', 'configuracion', 'Parámetros configurables de la organización', '/configuracion', 'cog', 4, TRUE)
ON CONFLICT (codigo) DO NOTHING;
