#!/usr/bin/env bash
# Conecta GitHub Actions con AWS sin claves: proveedor OIDC + rol "github-actions-deploy" con permisos mínimos.
# Se ejecuta UNA vez, con tu usuario administrador, después de crear el bucket y la distribución de CloudFront.
# Uso: ./deploy/aws/github-oidc.sh <usuario>/saas-platform <bucket-del-frontend> <id-distribución-cloudfront>
# Requiere AWS CLI v2 y jq. Puedes volver a ejecutarlo: actualiza el rol si ya existe.
set -euo pipefail
REPO=${1:?Uso: github-oidc.sh <usuario>/saas-platform <bucket> <id-distribución>}
BUCKET=${2:?Falta el nombre del bucket del frontend}
DIST_ID=${3:?Falta el ID de la distribución de CloudFront}
REGION=${REGION:-$(aws configure get region 2>/dev/null || true)}
: "${REGION:?Define la región: aws configure set region us-east-2}"
CLUSTER=${CLUSTER:-saas-prod}
ROL=github-actions-deploy
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
PROVEEDOR="arn:aws:iam::$ACCOUNT:oidc-provider/token.actions.githubusercontent.com"
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

# 1) Proveedor OIDC de GitHub (uno por cuenta)
if aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$PROVEEDOR" > /dev/null 2>&1; then
  echo "Proveedor OIDC ya existe"
else
  aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 1c58a3a8518e8759bf075b76b750d4f2df264fcd > /dev/null
  echo "Proveedor OIDC creado"
fi

# 2) Confianza: solo este repo, rama main o environment "production"
jq -n --arg p "$PROVEEDOR" --arg r "$REPO" '{
  Version: "2012-10-17",
  Statement: [{
    Effect: "Allow",
    Principal: { Federated: $p },
    Action: "sts:AssumeRoleWithWebIdentity",
    Condition: {
      StringEquals: { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      StringLike:   { "token.actions.githubusercontent.com:sub": [
        ("repo:" + $r + ":environment:production"),
        ("repo:" + $r + ":ref:refs/heads/main") ] }
    }
  }]
}' > "$TMP/confianza.json"

if aws iam get-role --role-name "$ROL" > /dev/null 2>&1; then
  aws iam update-assume-role-policy --role-name "$ROL" --policy-document "file://$TMP/confianza.json"
  echo "Rol $ROL actualizado"
else
  aws iam create-role --role-name "$ROL" --assume-role-policy-document "file://$TMP/confianza.json" \
    --description "Despliegues de GitHub Actions ($REPO)" --max-session-duration 3600 > /dev/null
  echo "Rol $ROL creado"
fi

# 3) Permisos mínimos: ECR (saas/*), ECS (cluster saas-prod), PassRole del rol de ejecución, S3 del frontend, invalidación
jq -n --arg a "$ACCOUNT" --arg reg "$REGION" --arg c "$CLUSTER" --arg b "$BUCKET" --arg d "$DIST_ID" '{
  Version: "2012-10-17",
  Statement: [
    { Sid: "EcrLogin", Effect: "Allow", Action: "ecr:GetAuthorizationToken", Resource: "*" },
    { Sid: "EcrPush", Effect: "Allow",
      Action: ["ecr:BatchCheckLayerAvailability","ecr:BatchGetImage","ecr:GetDownloadUrlForLayer",
               "ecr:InitiateLayerUpload","ecr:UploadLayerPart","ecr:CompleteLayerUpload","ecr:PutImage"],
      Resource: ("arn:aws:ecr:" + $reg + ":" + $a + ":repository/saas/*") },
    { Sid: "EcsTaskDefinitions", Effect: "Allow",
      Action: ["ecs:DescribeTaskDefinition","ecs:RegisterTaskDefinition"], Resource: "*" },
    { Sid: "EcsServicios", Effect: "Allow",
      Action: ["ecs:UpdateService","ecs:DescribeServices"],
      Resource: ("arn:aws:ecs:" + $reg + ":" + $a + ":service/" + $c + "/*") },
    { Sid: "PasarRolDeEjecucion", Effect: "Allow", Action: "iam:PassRole",
      Resource: ("arn:aws:iam::" + $a + ":role/ecsTaskExecutionRole"),
      Condition: { StringEquals: { "iam:PassedToService": "ecs-tasks.amazonaws.com" } } },
    { Sid: "FrontendListar", Effect: "Allow", Action: "s3:ListBucket", Resource: ("arn:aws:s3:::" + $b) },
    { Sid: "FrontendArchivos", Effect: "Allow", Action: ["s3:GetObject","s3:PutObject","s3:DeleteObject"],
      Resource: ("arn:aws:s3:::" + $b + "/*") },
    { Sid: "Invalidar", Effect: "Allow", Action: "cloudfront:CreateInvalidation",
      Resource: ("arn:aws:cloudfront::" + $a + ":distribution/" + $d) }
  ]
}' > "$TMP/permisos.json"
aws iam put-role-policy --role-name "$ROL" --policy-name despliegue-saas --policy-document "file://$TMP/permisos.json"

ROLE_ARN=$(aws iam get-role --role-name "$ROL" --query Role.Arn --output text)
cat <<FIN

Listo. En GitHub: repo $REPO -> Settings -> Secrets and variables -> Actions -> Variables, crea:
  AWS_ROLE_ARN                = $ROLE_ARN
  AWS_REGION                  = $REGION
  ECS_CLUSTER                 = $CLUSTER
  FRONTEND_BUCKET             = $BUCKET
  CLOUDFRONT_DISTRIBUTION_ID  = $DIST_ID

O con GitHub CLI:
  gh variable set AWS_ROLE_ARN --repo $REPO --body "$ROLE_ARN"
  gh variable set AWS_REGION --repo $REPO --body "$REGION"
  gh variable set ECS_CLUSTER --repo $REPO --body "$CLUSTER"
  gh variable set FRONTEND_BUCKET --repo $REPO --body "$BUCKET"
  gh variable set CLOUDFRONT_DISTRIBUTION_ID --repo $REPO --body "$DIST_ID"
FIN
