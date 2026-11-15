#!/bin/sh
# Migrador de la base de datos. Corre como tarea única de ECS (o en local) y es idempotente:
#   1. Si la base está vacía, carga schema.sql (equivale a V2…V25).
#   2. Aplica en orden las migraciones V26 en adelante que falten (tabla public.schema_migraciones).
#   3. Crea o actualiza el usuario de aplicación saas_app (solo lectura/escritura de datos) y sus permisos.
#   4. (Opcional) Crea el superadministrador si SUPERADMIN_EMAIL está definido y aún no existe.
# Variables: PGHOST, PGUSER, PGPASSWORD (usuario maestro), DB_APP_PASSWORD,
#            [PGDATABASE=saas_platform] [PGPORT=5432] [PGSSLMODE=require]
#            [SUPERADMIN_EMAIL] [SUPERADMIN_PASSWORD] [SUPERADMIN_NOMBRE] [SUPERADMIN_APELLIDO]
set -eu
: "${PGHOST:?Falta PGHOST}" "${PGUSER:?Falta PGUSER}" "${PGPASSWORD:?Falta PGPASSWORD}" "${DB_APP_PASSWORD:?Falta DB_APP_PASSWORD}"
export PGDATABASE="${PGDATABASE:-saas_platform}" PGSSLMODE="${PGSSLMODE:-require}" PGPORT="${PGPORT:-5432}"
DIR="${MIGRACIONES_DIR:-/migraciones}"
BASE=25                                   # schema.sql ya contiene V2…V25
PSQL="psql -X -q -v ON_ERROR_STOP=1"

echo "Conectando a $PGHOST/$PGDATABASE como $PGUSER"
i=0; until pg_isready -q; do i=$((i+1)); [ $i -ge 30 ] && { echo "La base no responde" >&2; exit 1; }; sleep 5; done

$PSQL -c "CREATE TABLE IF NOT EXISTS public.schema_migraciones (
            version     INTEGER PRIMARY KEY,
            archivo     TEXT        NOT NULL,
            aplicada_en TIMESTAMPTZ NOT NULL DEFAULT now())"
registradas=$($PSQL -tAc "SELECT count(*) FROM public.schema_migraciones")
existe=$($PSQL -tAc "SELECT to_regclass('gets_shared.organizaciones') IS NOT NULL")

# 1) Base nueva o cargada a mano antes del migrador
if [ "$registradas" = "0" ]; then
  if [ "$existe" = "f" ]; then
    echo "Base vacía: cargando schema.sql (V2…V$BASE)"
    $PSQL -f "$DIR/schema.sql"
  else
    echo "Las tablas ya existen pero no hay registro de migraciones: se toma la base como V$BASE"
  fi
  $PSQL -c "INSERT INTO public.schema_migraciones (version, archivo) VALUES ($BASE, 'schema.sql (V2…V$BASE)')"
fi

# 2) Migraciones pendientes (V26 en adelante), cada una en su propia transacción
ultima=$($PSQL -tAc "SELECT COALESCE(max(version), 0) FROM public.schema_migraciones")
pendientes=0
for f in "$DIR"/versiones/V*__*.sql; do
  [ -e "$f" ] || continue
  n=$(basename "$f" | sed -n 's/^V\([0-9][0-9]*\)__.*\.sql$/\1/p')
  [ -n "$n" ] && echo "$n $f"
done | sort -n | while read -r v f; do
  [ "$v" -le "$BASE" ] && continue
  ya=$($PSQL -tAc "SELECT count(*) FROM public.schema_migraciones WHERE version = $v")
  [ "$ya" != "0" ] && continue
  echo "Aplicando V$v: $(basename "$f")"
  $PSQL -1 -f "$f" -c "INSERT INTO public.schema_migraciones (version, archivo) VALUES ($v, '$(basename "$f")')"
done
echo "Versión de la base: V$($PSQL -tAc "SELECT max(version) FROM public.schema_migraciones")"

# 3) Usuario de aplicación: lee y escribe datos, no cambia la estructura
$PSQL -v clave="$DB_APP_PASSWORD" -v base="$PGDATABASE" <<'SQL'
SELECT 'CREATE ROLE saas_app LOGIN' WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'saas_app') \gexec
ALTER ROLE saas_app WITH LOGIN PASSWORD :'clave';
GRANT CONNECT ON DATABASE :"base" TO saas_app;
GRANT USAGE ON SCHEMA gets_shared TO saas_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA gets_shared TO saas_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA gets_shared TO saas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA gets_shared GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO saas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA gets_shared GRANT USAGE, SELECT ON SEQUENCES TO saas_app;
ALTER ROLE saas_app SET search_path TO gets_shared, public;
SQL
echo "Usuario saas_app listo"

# 4) Superadministrador (solo la primera vez)
if [ -n "${SUPERADMIN_EMAIL:-}" ]; then
  : "${SUPERADMIN_PASSWORD:?Falta SUPERADMIN_PASSWORD para crear el superadministrador}"
  $PSQL -v email="$SUPERADMIN_EMAIL" -v clave="$SUPERADMIN_PASSWORD" \
        -v nombre="${SUPERADMIN_NOMBRE:-Super}" -v apellido="${SUPERADMIN_APELLIDO:-Administrador}" <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
SET search_path TO gets_shared, public;
WITH u AS (
  INSERT INTO usuarios (email, password_hash, activo, email_verificado, organizacion_id, rol_id)
  SELECT lower(:'email'), crypt(:'clave', gen_salt('bf', 10)), TRUE, TRUE,
         (SELECT id FROM organizaciones WHERE es_plataforma),
         (SELECT id FROM roles WHERE nombre = 'Superadministrador' AND organizacion_id IS NULL)
  WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = lower(:'email'))
  RETURNING id)
INSERT INTO perfiles (usuario_id, nombre, apellido) SELECT id, :'nombre', :'apellido' FROM u;
SQL
  echo "Superadministrador verificado: $SUPERADMIN_EMAIL"
fi
echo "Migración terminada"
