#!/usr/bin/env bash
# Despliega una imagen nueva en ECS. Uso: TAG=20261120-0900 ./actualizar-servicio.sh catalog-service
# Requiere jq.
set -euo pipefail
S=${1:?Uso: TAG=<etiqueta> ./actualizar-servicio.sh <servicio>}
: "${TAG:?Define TAG con la etiqueta que imprimió build-push.sh}"
REGION=${REGION:-$(aws configure get region 2>/dev/null || true)}   # la de "aws configure" (us-east-2 en cuentas nuevas desde Colombia)
: "${REGION:?Define la región: aws configure set region us-east-2}"
CLUSTER=${CLUSTER:-saas-prod}
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
IMAGEN="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/saas/$S:$TAG"
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

aws ecs describe-task-definition --region "$REGION" --task-definition "saas-$S" --query taskDefinition > "$TMP/td.json"
jq --arg img "$IMAGEN" '.containerDefinitions[0].image = $img
  | del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy)' \
  "$TMP/td.json" > "$TMP/td-nueva.json"
ARN=$(aws ecs register-task-definition --region "$REGION" --cli-input-json "file://$TMP/td-nueva.json" \
      --query taskDefinition.taskDefinitionArn --output text)
aws ecs update-service --region "$REGION" --cluster "$CLUSTER" --service "$S" --task-definition "$ARN" > /dev/null
echo "Desplegando $ARN ..."
aws ecs wait services-stable --region "$REGION" --cluster "$CLUSTER" --services "$S" && echo "Listo: $S estable"
