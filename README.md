# SaaS Platform

Plataforma multi-tenant para venta de celulares: 4 microservicios Spring Boot (Java 25), un frontend React con Module Federation y PostgreSQL (esquema `gets_shared`).

## Estructura

| Carpeta | Qué contiene |
| --- | --- |
| `libs/platform-email`, `libs/platform-notifications` | Librerías compartidas (correo con SendGrid, plantillas, cola de notificaciones) |
| `services/user-service` | Usuarios, organizaciones, roles, verificación de correo · puerto 8082 |
| `services/catalog-service` | Productos, unidades, inventario, ventas · puerto 8083 |
| `services/subscription-service` | Planes, suscripciones, avisos y bloqueo por vencimiento · puerto 8084 |
| `services/notification-service` | Consume la cola de RabbitMQ y envía correos y WhatsApp · puerto 8085 |
| `frontend/` | Workspace npm: `ui-shell` + apps `security` (host), `catalog` y `subscription` |
| `database/schema.sql` | Esquema completo consolidado (V2…V25). `database/migrations/` guarda el historial |
| `deploy/docker/Dockerfile.backend` | Una sola imagen para los 4 servicios (`--build-arg SERVICE=...`) |
| `deploy/aws/` | Scripts de AWS: task definitions sin dominio, despliegue manual, conexión con GitHub (OIDC) |
| `.github/workflows/` | Pipelines de GitHub Actions |

## Desarrollo local

```bash
cp .env.example .env                         # opcional: SENDGRID_API_KEY, etc.
docker compose up --build                    # todo: frontend en http://localhost:3000
docker compose up --build postgres user-service   # solo lo que necesites
docker compose down -v                       # borra la base local; al subir se recarga schema.sql
```

Sin Docker: instala primero las librerías (`for d in libs/*/; do mvn -f "$d/pom.xml" -DskipTests install; done`) y luego corre cada servicio con `mvn spring-boot:run`.

## Pipelines (GitHub Actions)

| Workflow | Cuándo corre | Qué hace |
| --- | --- | --- |
| **Backend** | PR o push a `main` que toque `libs/`, `services/` o el Dockerfile; o manual | PR: compila. `main`: compila, sube la imagen a ECR (`saas/<servicio>:<commit>`) y despliega en ECS esperando a que quede estable. Solo los servicios que cambiaron; un cambio en `libs/` despliega los 4 |
| **Frontend** | PR o push a `main` que toque `frontend/`; o manual | PR: compila. `main`: compila, sube a S3 e invalida CloudFront |

Para desplegar a mano: pestaña **Actions → Backend → Run workflow** y elige el servicio (o `todos`).

Los pipelines no guardan claves de AWS: GitHub pide credenciales temporales por OIDC al rol `github-actions-deploy`, que solo puede subir imágenes `saas/*`, actualizar servicios del cluster `saas-prod`, escribir en el bucket del frontend e invalidar su distribución. Mientras la variable `AWS_ROLE_ARN` no exista, los pipelines solo compilan.

### Conectar con AWS (una vez)

Requisito: la infraestructura de la guía de despliegue ya creada (cluster `saas-prod`, los 4 servicios en ECS con su task definition, repos de ECR, bucket y CloudFront). El pipeline toma la task definition activa de cada servicio y solo cambia la imagen, así que las variables y secretos se siguen administrando con `deploy/aws/sin-dominio/registrar-taskdefs.sh`.

1. Con tu usuario administrador de AWS:

   ```bash
   ./deploy/aws/github-oidc.sh <tu-usuario>/saas-platform <bucket-del-frontend> <id-distribución-cloudfront>
   ```

2. El script imprime 5 variables. Créalas en **Settings → Secrets and variables → Actions → Variables** (o con los comandos `gh variable set` que imprime):

   | Variable | Ejemplo |
   | --- | --- |
   | `AWS_ROLE_ARN` | `arn:aws:iam::123456789012:role/github-actions-deploy` |
   | `AWS_REGION` | `us-east-2` |
   | `ECS_CLUSTER` | `saas-prod` |
   | `FRONTEND_BUCKET` | `saas-frontend-123456789012` |
   | `CLOUDFRONT_DISTRIBUTION_ID` | `E1ABCDEF2GHIJK` |

3. (Recomendado) En **Settings → Environments → production**, agrega *Required reviewers*: cada despliegue esperará tu aprobación. Y en **Settings → Branches**, protege `main` para que los cambios entren por pull request.
4. Prueba: **Actions → Backend → Run workflow → todos**.

### Cambios de configuración

- **Variables de entorno o secretos de un servicio:** edita `deploy/aws/sin-dominio/config.env` o Parameter Store, corre `registrar-taskdefs.sh` y luego **Run workflow** para ese servicio (o *Force new deployment* en ECS).
- **Base de datos:** las migraciones no se aplican solas. Agrega `database/migrations/V26__...sql`, ejecútala en RDS y súmala a `schema.sql`.
