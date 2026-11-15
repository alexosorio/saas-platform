#!/usr/bin/env bash
# Escribe .env.production en las 3 apps del frontend para el despliegue SIN dominio.
# Rutas relativas: el frontend y las APIs salen por la misma dirección de CloudFront,
# así que no hay que recompilar cuando cambie el dominio.
# Uso (desde la raíz del repo):  ./deploy/aws/sin-dominio/env-frontend.sh
set -euo pipefail
cd "$(dirname "$0")/../../.."
WS=frontend
[ -d "$WS/apps" ] || { echo "No encuentro $WS/apps junto a este script" >&2; exit 1; }
for app in security catalog subscription; do
  cat > "$WS/apps/$app/.env.production" <<'ENV'
VITE_API_URL=/api-usuarios
VITE_USER_API_URL=/api-usuarios
VITE_CATALOG_API_URL=/api-catalogo
VITE_SUBSCRIPTION_API_URL=/api-suscripciones
VITE_INACTIVITY_MINUTES=30
ENV
  echo "OK  $WS/apps/$app/.env.production"
done
