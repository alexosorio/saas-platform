#!/usr/bin/env bash
# Registra en ECS las task definitions del despliegue SIN dominio, con los valores de config.env.
# Uso:  ./deploy/aws/sin-dominio/registrar-taskdefs.sh                    -> los 4 servicios
#       ./deploy/aws/sin-dominio/registrar-taskdefs.sh rabbitmq           -> RabbitMQ (si no lo creaste con el JSON de la guía)
#       ./deploy/aws/sin-dominio/registrar-taskdefs.sh catalog-service    -> solo una
#       SOLO_GENERAR=1 ./deploy/aws/sin-dominio/registrar-taskdefs.sh     -> solo escribe los JSON en generadas/ (para revisarlos)
# Requiere: AWS CLI v2 y jq.
set -euo pipefail
cd "$(dirname "$0")"

# config.env (tolera finales de línea de Windows)
[ -f config.env ] || { echo "Falta config.env" >&2; exit 1; }
while IFS= read -r linea || [ -n "$linea" ]; do
  linea=${linea%$'\r'}                                   # finales de línea de Windows
  [[ "$linea" =~ ^[[:space:]]*(#|$) ]] && continue
  [[ "$linea" =~ ^[[:space:]]*([A-Z_][A-Z0-9_]*)=(.*)$ ]] || { echo "config.env: línea inválida: $linea" >&2; exit 1; }
  k=${BASH_REMATCH[1]}; v=${BASH_REMATCH[2]}
  if [[ "$v" =~ ^\"([^\"]*)\" || "$v" =~ ^\'([^\']*)\' ]]; then
    v=${BASH_REMATCH[1]}                                  # valor entre comillas
  else
    v=${v%%[[:space:]]#*}                                 # sin comentario al final ("valor   # nota")
    v=${v%"${v##*[![:space:]]}"}                          # sin espacios al final
  fi
  [ -z "$v" ] && [ -n "${!k:-}" ] && continue             # vacío en config.env: respeta la variable de entorno
  printf -v "$k" '%s' "$v"
done < config.env

REGION=${REGION:-$(aws configure get region 2>/dev/null || true)}
CF_DOMAIN=${CF_DOMAIN#https://}; CF_DOMAIN=${CF_DOMAIN#http://}; CF_DOMAIN=${CF_DOMAIN%/}
for v in REGION CF_DOMAIN RDS_ENDPOINT EFS_ID TAG MAIL_FROM MAIL_FROM_NAME BRAND_NAME; do
  [ -n "${!v:-}" ] || { echo "config.env: falta $v${v/#REGION/ (o configúrala: aws configure set region us-east-2)}" >&2; exit 1; }
done
case "$CF_DOMAIN$RDS_ENDPOINT$EFS_ID" in
  *d1a2b3c4d5e6f7*|*xxxxxxxxxxxx*|*0123456789abcdef0*) echo "config.env todavía tiene valores de ejemplo (CF_DOMAIN, RDS_ENDPOINT o EFS_ID)" >&2; exit 1 ;;
esac

if [ "${SOLO_GENERAR:-}" = "1" ]; then ACCOUNT=${ACCOUNT:-123456789012}
else ACCOUNT=$(aws sts get-caller-identity --query Account --output text); fi

if [ $# -gt 0 ]; then LISTA=("$@")
else LISTA=(user-service catalog-service subscription-service notification-service); fi

# Los secretos deben existir en Parameter Store, o la tarea no arranca
if [ "${SOLO_GENERAR:-}" != "1" ]; then
  FALTAN=$(aws ssm get-parameters --region "$REGION" --query InvalidParameters --output text --names \
    /saas/prod/DB_PASSWORD /saas/prod/JWT_SECRET /saas/prod/RABBITMQ_PASSWORD \
    /saas/prod/SENDGRID_API_KEY /saas/prod/WHATSAPP_ACCESS_TOKEN)
  if [ -n "$FALTAN" ] && [ "$FALTAN" != "None" ]; then
    echo "Faltan parámetros en Parameter Store: $FALTAN" >&2
    echo "Créalos (si no usas WhatsApp, WHATSAPP_ACCESS_TOKEN puede valer 'pendiente')." >&2
    exit 1
  fi
fi

mkdir -p generadas
for s in "${LISTA[@]}"; do
  plantilla="taskdefs/$s.json"
  [ -f "$plantilla" ] || { echo "No existe $plantilla" >&2; exit 1; }
  jq --arg ACCOUNT "$ACCOUNT" --arg REGION "$REGION" --arg TAG "$TAG" \
     --arg CF_DOMAIN "$CF_DOMAIN" --arg RDS_ENDPOINT "$RDS_ENDPOINT" --arg EFS_ID "$EFS_ID" \
     --arg MAIL_FROM "$MAIL_FROM" --arg MAIL_FROM_NAME "$MAIL_FROM_NAME" --arg BRAND_NAME "$BRAND_NAME" \
     --arg WA_ID "${WHATSAPP_PHONE_NUMBER_ID:-}" --arg WA_VER "${WHATSAPP_API_VERSION:-v21.0}" '
    def r: gsub("__ACCOUNT__"; $ACCOUNT) | gsub("__REGION__"; $REGION) | gsub("__TAG__"; $TAG)
         | gsub("__CF_DOMAIN__"; $CF_DOMAIN) | gsub("__RDS_ENDPOINT__"; $RDS_ENDPOINT) | gsub("__EFS_ID__"; $EFS_ID)
         | gsub("__MAIL_FROM_NAME__"; $MAIL_FROM_NAME) | gsub("__MAIL_FROM__"; $MAIL_FROM)
         | gsub("__BRAND_NAME__"; $BRAND_NAME)
         | gsub("__WHATSAPP_PHONE_NUMBER_ID__"; $WA_ID) | gsub("__WHATSAPP_API_VERSION__"; $WA_VER);
    walk(if type == "string" then r else . end)' "$plantilla" > "generadas/$s.json"
  if grep -q '__[A-Z_]*__' "generadas/$s.json"; then echo "Quedó un valor sin reemplazar en generadas/$s.json" >&2; exit 1; fi

  if [ "${SOLO_GENERAR:-}" = "1" ]; then echo "Generada: generadas/$s.json"; continue; fi
  ARN=$(aws ecs register-task-definition --region "$REGION" --cli-input-json "file://generadas/$s.json" \
        --query taskDefinition.taskDefinitionArn --output text)
  echo "OK  $s -> $ARN"
done
