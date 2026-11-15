#!/usr/bin/env bash
# Crea o actualiza un stack de CloudFormation con su plantilla y sus parámetros del repo.
#   infra/scripts/desplegar-stack.sh base
#   infra/scripts/desplegar-stack.sh app CatalogImageTag=abc123     (los demás parámetros conservan su valor)
# Espera si el stack está ocupado por otro despliegue y reintenta, así dos pipelines nunca se pisan.
# Variables: AWS_REGION (o la de aws configure), CFN_ROLE_ARN (por defecto el rol saas-cloudformation).
set -euo pipefail
cd "$(dirname "$0")/.."                       # carpeta infra/
NOMBRE=${1:?Uso: desplegar-stack.sh <base|app> [Clave=Valor ...]}; shift
STACK="saas-$NOMBRE"
PLANTILLA="$NOMBRE.yml"
PARAMS_FILE="parametros/$NOMBRE.json"
[ -f "$PLANTILLA" ] || { echo "No existe infra/$PLANTILLA" >&2; exit 1; }
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
CFN_ROLE_ARN=${CFN_ROLE_ARN:-arn:aws:iam::$ACCOUNT:role/saas-cloudformation}

estado() { aws cloudformation describe-stacks --stack-name "$STACK" --query 'Stacks[0].StackStatus' --output text 2>/dev/null || echo NO_EXISTE; }

esperar_libre() {
  local s; for _ in $(seq 1 120); do
    s=$(estado)
    case "$s" in *_IN_PROGRESS) echo "  $STACK está en $s; esperando 30 s..."; sleep 30 ;; *) echo "$s"; return 0 ;; esac
  done
  echo "  $STACK sigue ocupado después de 60 minutos" >&2; return 1
}

eventos_fallidos() {
  echo "Últimos errores de $STACK:"
  # Todos los eventos (el rollback genera decenas) y sin los "cancelled", que son consecuencia del primero
  aws cloudformation describe-stack-events --stack-name "$STACK" \
    --query 'StackEvents[?ends_with(ResourceStatus, `FAILED`) && ResourceStatusReason != `Resource creation cancelled`].[Timestamp, LogicalResourceId, ResourceStatus, ResourceStatusReason]' \
    --output text 2>/dev/null | head -15 || true
}

# Parámetros: archivo del repo + los que lleguen como argumentos (estos ganan)
PARAMS=()
if [ -f "$PARAMS_FILE" ]; then
  while IFS= read -r linea; do PARAMS+=("$linea"); done < <(jq -r 'to_entries[] | "\(.key)=\(.value)"' "$PARAMS_FILE")
fi
PARAMS+=("$@")

for intento in 1 2 3; do
  s=$(esperar_libre | tail -1)
  if [ "$s" = "ROLLBACK_COMPLETE" ]; then
    echo "La creación anterior de $STACK falló (ROLLBACK_COMPLETE); se borra para volver a crearlo"
    eventos_fallidos
    aws cloudformation delete-stack --stack-name "$STACK" --role-arn "$CFN_ROLE_ARN"
    aws cloudformation wait stack-delete-complete --stack-name "$STACK"
  fi
  echo "Desplegando $STACK ($PLANTILLA) — intento $intento"
  set +e
  salida=$(aws cloudformation deploy --stack-name "$STACK" --template-file "$PLANTILLA" \
    --role-arn "$CFN_ROLE_ARN" --capabilities CAPABILITY_NAMED_IAM --no-fail-on-empty-changeset \
    --tags Proyecto=saas-platform Entorno=produccion \
    ${PARAMS[@]+--parameter-overrides "${PARAMS[@]}"} 2>&1)
  codigo=$?
  set -e
  echo "$salida"
  [ $codigo -eq 0 ] && { echo "OK  $STACK: $(estado)"; exit 0; }
  if echo "$salida" | grep -q -E "_IN_PROGRESS state and can not be updated|is in .*_IN_PROGRESS"; then
    echo "Otro despliegue tomó el stack; se reintenta"; continue
  fi
  eventos_fallidos; exit $codigo
done
echo "No se pudo desplegar $STACK después de 3 intentos" >&2; exit 1
