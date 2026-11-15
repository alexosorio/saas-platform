-- V15: administración de suscripciones de la plataforma
--  - Validación de pagos (quién, cuándo, motivo de rechazo)
--  - Acción 'suspension' en el historial (manual y automática por no pago)
--  - Módulo de plataforma y rol Superadministrador
SET search_path TO gets_shared;

ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS motivo_rechazo   text,
    ADD COLUMN IF NOT EXISTS validado_por     bigint,
    ADD COLUMN IF NOT EXISTS fecha_validacion timestamp;

ALTER TYPE accion_suscripcion ADD VALUE IF NOT EXISTS 'suspension';

-- Módulo de plataforma: el comodín '*' de los administradores de cada organización NO lo concede
INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Administración de suscripciones', 'plataforma_suscripciones',
        'Suscripciones, validación de pagos y facturación de TODAS las organizaciones (solo plataforma)',
        '/subscription/admin', 'crown', 90, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- Rol de la plataforma: acceso total ('*') + administración de suscripciones
INSERT INTO roles (nombre, descripcion, nivel, es_sistema, activo)
VALUES ('Superadministrador', 'Administrador de la plataforma (todas las organizaciones)', 10, TRUE, TRUE)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, m.codigo, m.nombre, TRUE, TRUE, TRUE, TRUE
FROM roles r JOIN modulos m ON m.codigo IN ('plataforma_suscripciones', '*')
WHERE r.nombre = 'Superadministrador'
ON CONFLICT (rol_id, modulo_id) DO NOTHING;

-- Asigna el rol a la persona que administra la plataforma (ajusta el correo).
-- Conserva todos sus permisos actuales, porque el rol incluye el comodín '*'.
-- UPDATE usuarios SET rol_id = (SELECT id FROM roles WHERE nombre = 'Superadministrador')
--  WHERE email = 'tu-correo@dominio.com';
