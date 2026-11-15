#!/usr/bin/env bash
# Lanza la tarea saas-db-migrator en ECS, espera a que termine y muestra su salida.
# Falla si el migrador termina con error. Requiere el stack saas-base.
set -euo pipefail
salida() { aws cloudformation describe-stacks --stack-name saas-base \
  --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text; }
CLUSTER=$(salida ClusterName)
TASKDEF=$(salida MigratorTaskDefinition)
SUBNETS=$(salida PublicSubnets)
SG=$(salida AppsSecurityGroup)

echo "Lanzando $TASKDEF en $CLUSTER"
TAREA=$(aws ecs run-task --cluster "$CLUSTER" --task-definition "$TASKDEF" --launch-type FARGATE --count 1 \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNETS],securityGroups=[$SG],assignPublicIp=ENABLED}" \
  --started-by github-actions --query 'tasks[0].taskArn' --output text)
[ -n "$TAREA" ] && [ "$TAREA" != "None" ] || { echo "No se pudo lanzar la tarea" >&2; exit 1; }
echo "Tarea: $TAREA"
aws ecs wait tasks-stopped --cluster "$CLUSTER" --tasks "$TAREA" || aws ecs wait tasks-stopped --cluster "$CLUSTER" --tasks "$TAREA"

CODIGO=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TAREA" --query 'tasks[0].containers[0].exitCode' --output text)
MOTIVO=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TAREA" --query 'tasks[0].stoppedReason' --output text)
ID=${TAREA##*/}
echo "----- salida del migrador -----"
aws logs get-log-events --log-group-name /ecs/saas-prod --log-stream-name "db-migrator/migrador/$ID" \
  --start-from-head --query 'events[].message' --output text 2>/dev/null | tr '\t' '\n' || echo "(sin logs: $MOTIVO)"
echo "-------------------------------"
[ "$CODIGO" = "0" ] || { echo "El migrador terminó con código $CODIGO ($MOTIVO)" >&2; exit 1; }
echo "Migración OK"
