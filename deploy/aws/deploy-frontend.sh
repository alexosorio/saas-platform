#!/usr/bin/env bash
# Compila el frontend y lo publica en S3 + CloudFront.
# Uso: BUCKET=saas-frontend-123456789012 DIST_ID=E1234567890ABC ./deploy/aws/deploy-frontend.sh
#      SKIP_BUILD=1 ... -> solo sube (el pipeline compila en un paso aparte)
set -euo pipefail
cd "$(dirname "$0")/../../frontend"
: "${BUCKET:?Define BUCKET (nombre del bucket del frontend)}"
: "${DIST_ID:?Define DIST_ID (ID de la distribución de CloudFront)}"
LARGO="public,max-age=604800,immutable"

if [ "${SKIP_BUILD:-}" != "1" ]; then
  npm ci
  npm run build:all
fi
for app in security catalog subscription; do
  [ -f "apps/$app/dist/index.html" ] || [ -f "apps/$app/dist/remoteEntry.js" ] || { echo "Falta apps/$app/dist (compila primero)" >&2; exit 1; }
done

# Host en la raíz, sin borrar las carpetas de los remotes
aws s3 sync apps/security/dist     "s3://$BUCKET/" --delete --exclude "catalog/*" --exclude "subscription/*" --cache-control "$LARGO"
aws s3 sync apps/catalog/dist      "s3://$BUCKET/catalog/"      --delete --cache-control "$LARGO"
aws s3 sync apps/subscription/dist "s3://$BUCKET/subscription/" --delete --cache-control "$LARGO"

# Puntos de entrada sin caché: así cada despliegue se ve de inmediato
aws s3 cp apps/security/dist/index.html          "s3://$BUCKET/index.html"                  --cache-control no-cache --content-type text/html
aws s3 cp apps/catalog/dist/remoteEntry.js       "s3://$BUCKET/catalog/remoteEntry.js"      --cache-control no-cache --content-type application/javascript
aws s3 cp apps/subscription/dist/remoteEntry.js  "s3://$BUCKET/subscription/remoteEntry.js" --cache-control no-cache --content-type application/javascript

aws cloudfront create-invalidation --distribution-id "$DIST_ID" \
  --paths "/index.html" "/catalog/remoteEntry.js" "/subscription/remoteEntry.js" --query Invalidation.Id --output text
