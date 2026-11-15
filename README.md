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
| `database/migrar.sh`, `database/Dockerfile` | Migrador: carga `schema.sql` en una base vacía y aplica V26+ en orden |
| `infra/` | CloudFormation: bootstrap (OIDC), `saas-base` y `saas-app`, parámetros y scripts de despliegue |
| `deploy/aws/` | Despliegue manual de imágenes (`build-push.sh`) y del frontend (`deploy-frontend.sh`) |
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
| **Infraestructura** | PR o push a `main` que toque `infra/` o `database/`; o manual | PR: valida las plantillas. `main`: aplica `saas-base`, migra la base y aplica `saas-app` (la primera vez crea toda la plataforma) |
| **Backend** | PR o push a `main` que toque `libs/`, `services/` o el Dockerfile; o manual | PR: compila. `main`: compila, sube la imagen a ECR (`saas/<servicio>:<commit>`) y cambia su etiqueta en `saas-app`. Solo los servicios que cambiaron; un cambio en `libs/` despliega los 4 |
| **Frontend** | PR o push a `main` que toque `frontend/`; o manual | PR: compila. `main`: compila, sube a S3 e invalida CloudFront |

Los pipelines no guardan claves de AWS: GitHub pide credenciales temporales por OIDC al rol `saas-github-deploy`. Mientras la variable `AWS_ROLE_ARN` no exista en GitHub, solo compilan y validan.

Toda la infraestructura está en `infra/` como plantillas de CloudFormation. Cómo crearla la primera vez y cómo cambiarla después: [infra/README.md](infra/README.md).

## Cambios de configuración

- **Variables de los servicios** (remitente, marca, WhatsApp): `infra/parametros/app.json` y push.
- **Secretos:** Parameter Store (`/saas/prod/*`); luego Actions → Backend → Run workflow para que las tareas los vuelvan a leer.
- **Base de datos:** agrega `database/migrations/V26__descripcion.sql`; el pipeline la aplica una sola vez.
