-- V23: logo propio de cada organización (opcional)
-- Se guarda normalizado a PNG/JPEG (el frontend convierte SVG a PNG) para que se vea en
-- todos los clientes de correo: Gmail y Outlook no muestran SVG.
SET search_path TO gets_shared;

ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS logo             BYTEA;
ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS logo_tipo        VARCHAR(30);
ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS logo_actualizado TIMESTAMP;

COMMENT ON COLUMN organizaciones.logo IS 'Logo de la organización (PNG o JPEG, máx. 300 KB). Se usa en correos, menú y páginas públicas.';
