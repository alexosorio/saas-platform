#!/usr/bin/env bash
# Comprueba que existan en Parameter Store los secretos que usan las tareas (sin leer sus valores).
# Si falta alguno, ECS no puede arrancar las tareas, así que el pipeline se detiene antes.
set -euo pipefail
cd "$(dirname "$0")/.."
REQUERIDOS=(DB_PASSWORD JWT_SECRET RABBITMQ_PASSWORD SENDGRID_API_KEY WHATSAPP_ACCESS_TOKEN)
if [ -n "$(jq -r '.SuperadminEmail // ""' parametros/base.json)" ]; then REQUERIDOS+=(SUPERADMIN_PASSWORD); fi
EXISTEN=$(aws ssm describe-parameters --parameter-filters "Key=Name,Option=BeginsWith,Values=/saas/prod/" \
  --query 'Parameters[].Name' --output text | tr '\t' '\n')
FALTAN=()
for p in "${REQUERIDOS[@]}"; do grep -qx "/saas/prod/$p" <<< "$EXISTEN" || FALTAN+=("/saas/prod/$p"); done
if [ ${#FALTAN[@]} -gt 0 ]; then
  echo "Faltan parámetros en Parameter Store (región $(aws configure get region 2>/dev/null || echo "$AWS_REGION")):" >&2
  printf '  %s\n' "${FALTAN[@]}" >&2
  echo "Créalos con: aws ssm put-parameter --type SecureString --name <nombre> --value <valor>" >&2
  exit 1
fi
echo "Secretos OK: ${REQUERIDOS[*]}"
