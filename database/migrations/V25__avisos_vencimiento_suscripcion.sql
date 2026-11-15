-- V25: avisos antes del vencimiento de la suscripción y bloqueo inmediato al vencer
SET search_path TO gets_shared;

-- Registro de avisos enviados: evita repetir el mismo aviso para el mismo vencimiento
CREATE TABLE IF NOT EXISTS avisos_suscripcion (
    id              BIGSERIAL PRIMARY KEY,
    suscripcion_id  BIGINT      NOT NULL REFERENCES suscripciones(id),
    organizacion_id BIGINT      NOT NULL,
    tipo            VARCHAR(20) NOT NULL,          -- previo | suspendida
    fecha_fin       DATE        NOT NULL,          -- vencimiento al que se refiere el aviso
    dias_antes      INTEGER     NOT NULL DEFAULT 0,
    destinatarios   TEXT,
    enviado_en      TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT ux_aviso_suscripcion UNIQUE (suscripcion_id, tipo, fecha_fin, dias_antes)
);
CREATE INDEX IF NOT EXISTS idx_avisos_suscripcion_org ON avisos_suscripcion (organizacion_id, enviado_en DESC);

-- Días antes del vencimiento en que se avisa (lista separada por comas)
INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion) VALUES
 ('suscripcion.dias_aviso_vencimiento', '7,3,1,0', 'texto', 'plataforma', 'Suscripción',
  'Avisos antes del vencimiento (días)',
  'Días antes del vencimiento en que se envía un correo a los administradores de la organización. Separados por comas; 0 = el mismo día.')
ON CONFLICT DO NOTHING;

-- Bloqueo inmediato: sin días de gracia (la suscripción se suspende el día siguiente a su fecha de fin)
UPDATE parametros
   SET valor = '0',
       descripcion = 'Días después del vencimiento antes de suspender una suscripción sin pago. 0 = bloqueo inmediato al vencer.'
 WHERE clave = 'suscripcion.dias_gracia' AND organizacion_id IS NULL;
