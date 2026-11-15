# Infraestructura (CloudFormation)

Toda la plataforma en AWS se crea y se actualiza desde estas plantillas. El pipeline **Infraestructura** (`.github/workflows/infra.yml`) las aplica en cada push a `main` que toque `infra/` o `database/`.

| Stack | Plantilla | Contenido | Cómo se despliega |
| --- | --- | --- | --- |
| `saas-bootstrap` | `bootstrap.yml` | Proveedor OIDC de GitHub, rol `saas-github-deploy` (pipelines) y rol `saas-cloudformation` (crea la infraestructura) | **A mano, una vez** |
| `saas-base` | `base.yml` | VPC, grupos de seguridad, ECR, PostgreSQL (RDS), EFS, cluster ECS, Cloud Map, logs, bucket del frontend, tarea del migrador | Pipeline |
| `saas-app` | `app.yml` | Balanceador interno, RabbitMQ, los 4 servicios, CloudFront (frontend + APIs por origen VPC) | Pipeline |

Los valores que cambias tú están en `parametros/base.json` y `parametros/app.json`. Los secretos nunca van en el repo: viven en Parameter Store (`/saas/prod/*`) y las tareas los leen al arrancar.

## Primera vez

Todo en la región de tu cuenta (`us-east-2` para cuentas nuevas desde Colombia) y con tu usuario administrador.

1. **Secretos en Parameter Store.** Si ya los creaste con la guía, solo agrega los que falten:

   ```bash
   aws configure set region us-east-2
   P=/saas/prod
   put() { aws ssm put-parameter --name "$P/$1" --type SecureString --value "$2" --overwrite; }
   put DB_PASSWORD "$(openssl rand -base64 32 | tr -d '/+=@')"
   put JWT_SECRET "$(openssl rand -base64 64 | tr -d '\n')"
   put RABBITMQ_PASSWORD "$(openssl rand -base64 24 | tr -d '/+=@')"
   put SENDGRID_API_KEY 'SG.tu-api-key'
   put WHATSAPP_ACCESS_TOKEN 'pendiente'
   put SUPERADMIN_PASSWORD 'UnaClaveTemporalLarga!2026'    # solo si usas SuperadminEmail
   ```

   La contraseña maestra de RDS no hace falta: la genera y la guarda AWS (Secrets Manager).

2. **Tus valores.** En `parametros/app.json` pon en `MailFrom` el remitente que verificaste en SendGrid. En `parametros/base.json` pon tu correo en `SuperadminEmail` para que el migrador cree el superadministrador (vacío = no lo crea). Haz commit, pero todavía sin conectar AWS.

3. **Bootstrap** (desde la raíz del repo):

   ```bash
   aws cloudformation deploy --stack-name saas-bootstrap --template-file infra/bootstrap.yml \
     --capabilities CAPABILITY_NAMED_IAM --parameter-overrides GitHubRepo=alexosorio/saas-platform
   aws cloudformation describe-stacks --stack-name saas-bootstrap \
     --query "Stacks[0].Outputs[?OutputKey=='GitHubDeployRoleArn'].OutputValue" --output text
   ```

   Si la cuenta ya tenía el proveedor OIDC de GitHub, agrega `CrearProveedorOidc=false`. Los IDs numéricos del dueño y del repo ya vienen por defecto (`GitHubOwnerId`, `GitHubRepoId`); GitHub los usa en los tokens de repos creados desde julio de 2026.

4. **Variables en GitHub** (Settings → Secrets and variables → Actions → Variables):

   | Variable | Valor |
   | --- | --- |
   | `AWS_ROLE_ARN` | El ARN que imprimió el paso 3 (`arn:aws:iam::<cuenta>:role/saas-github-deploy`) |
   | `AWS_REGION` | `us-east-2` |

5. **Despliegue:** Actions → **Infraestructura** → Run workflow. La primera vez tarda unos 40 minutos (RDS ~10, CloudFront y el origen VPC ~20). El resumen del run muestra la dirección `https://dxxxx.cloudfront.net`.

## Después

| Cambias | Pasa |
| --- | --- |
| Código de un servicio | **Backend**: compila, sube la imagen y cambia su etiqueta en `saas-app`. ECS reemplaza las tareas sin corte y revierte solo si la nueva no arranca |
| `frontend/` | **Frontend**: compila, sube a S3 e invalida CloudFront |
| `infra/*.yml` o `parametros/` | **Infraestructura**: actualiza los stacks (las imágenes conservan su etiqueta) |
| `database/migrations/V26__...sql` | **Infraestructura**: el migrador la aplica una sola vez, en orden, antes de actualizar la app |

Para revisar un cambio de infraestructura antes de aplicarlo, ábrelo como pull request: el pipeline valida las plantillas sin tocar AWS.

### Migraciones de base de datos

- `database/schema.sql` equivale a V2…V25 y solo se carga en una base vacía.
- Las nuevas van en `database/migrations/` con el siguiente número (`V26__descripcion.sql`). Cada una corre en su propia transacción y queda registrada en `public.schema_migraciones`.
- Si la base ya tenía las tablas (cargada a mano), el migrador la toma como V25 sin tocar nada.
- Prueba local: `docker compose up postgres` y `PGHOST=localhost PGUSER=postgres PGPASSWORD=postgres PGSSLMODE=disable DB_APP_PASSWORD=x MIGRACIONES_DIR=<carpeta> sh database/migrar.sh`.

## Protecciones

- RDS tiene `DeletionProtection` y, si se reemplaza o borra, deja un snapshot.
- ECR, EFS y el bucket se conservan si se borra `saas-base` (salvo que la creación inicial falle).
- `saas-github-deploy` solo puede tocar stacks `saas-*`, subir imágenes `saas/*`, lanzar el migrador, escribir en el bucket `saas-frontend-*` e invalidar CloudFront. Los cambios de infraestructura los hace CloudFormation con `saas-cloudformation`.

Para borrar todo: primero `saas-app`, luego quita la protección de RDS y borra `saas-base`. `saas-bootstrap` va al final.
