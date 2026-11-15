-- V22: el Propietario administra toda su organización + enlace de verificación de correo
SET search_path TO gets_shared;

-- ─── 1) Propietario con acceso total (comodín '*') ──────────────────────
-- Antes solo tenía los módulos que existían al sembrar la base: le faltaban los que
-- se agregaron después (roles, configuración, auditoría, devoluciones…) y, si sus
-- permisos se editaron, podía quedar sin 'usuarios'. Con '*' cubre también los futuros.
-- Los permisos de PLATAFORMA (plataforma_*) no se conceden con '*'.
INSERT INTO modulos (nombre, codigo, descripcion, orden, activo)
VALUES ('Todos los módulos', '*', 'Comodín: acceso total a todos los módulos', 0, TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, '*', 'Todos los módulos', TRUE, TRUE, TRUE, TRUE
  FROM roles r JOIN modulos m ON m.codigo = '*'
 WHERE r.nombre = 'Propietario' AND r.organizacion_id IS NULL
ON CONFLICT (rol_id, modulo_id) DO UPDATE
   SET crear = TRUE, leer = TRUE, actualizar = TRUE, eliminar = TRUE,
       modulo_codigo = '*', modulo_nombre = 'Todos los módulos';

-- ─── 2) Enlace de verificación de correo ────────────────────────────────
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_verificacion        VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_verificacion_expira TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS verificacion_enviada_en   TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_token_verificacion
    ON usuarios (token_verificacion) WHERE token_verificacion IS NOT NULL;

INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion, minimo, maximo) VALUES
 ('seguridad.horas_enlace_verificacion', '48', 'entero', 'plataforma', 'Seguridad',
  'Vigencia del enlace de verificación (horas)',
  'Horas durante las que sirve el enlace de verificación de correo enviado al registrarse o al reenviarlo.', 1, 168)
ON CONFLICT DO NOTHING;

-- Verificación: el Propietario debe tener el comodín (1 fila)
SELECT r.id, r.nombre, rm.modulo_codigo, rm.leer
  FROM roles r JOIN roles_modulos rm ON rm.rol_id = r.id
 WHERE r.nombre = 'Propietario' AND r.organizacion_id IS NULL AND rm.modulo_codigo = '*';
