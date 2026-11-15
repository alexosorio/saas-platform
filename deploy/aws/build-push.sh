#!/usr/bin/env bash
# Construye y sube a ECR las imágenes de los backends DESDE TU MÁQUINA (el pipeline lo hace solo).
# Uso: ./deploy/aws/build-push.sh                 -> los 4 servicios
#      ./deploy/aws/build-push.sh catalog-service -> solo uno
set -euo pipefail
cd "$(dirname "$0")/../.."          # raíz del repo
REGION=${REGION:-$(aws configure get region 2>/dev/null || true)}
: "${REGION:?Define la región: aws configure set region us-east-2}"
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
TAG=${TAG:-$(date +%Y%m%d-%H%M)}
if [ $# -gt 0 ]; then SERVICIOS=("$@")
else SERVICIOS=(user-service catalog-service subscription-service notification-service); fi

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
for s in "${SERVICIOS[@]}"; do
  [ -f "services/$s/pom.xml" ] || { echo "No existe services/$s" >&2; exit 1; }
  docker build --platform linux/amd64 -f deploy/docker/Dockerfile.backend --build-arg SERVICE="$s" \
    -t "$REGISTRY/saas/$s:$TAG" .
  docker push "$REGISTRY/saas/$s:$TAG"
  echo "OK  $s -> $REGISTRY/saas/$s:$TAG"
done
echo "Etiqueta publicada: $TAG"
