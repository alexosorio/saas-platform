# 🚀 User Service - Patrón CQRS con Docker

Sistema de gestión de usuarios implementando **CQRS** (Command Query Responsibility Segregation), **Spring Boot 3**, **PostgreSQL**, totalmente **dockerizado**.

## 📐 Arquitectura CQRS

### ¿Qué es CQRS?

CQRS separa las operaciones de **escritura** (Commands) de las de **lectura** (Queries):

```
┌─────────────────┐         ┌──────────────────┐
│   COMMANDS      │         │     QUERIES      │
│   (Write)       │         │     (Read)       │
├─────────────────┤         ├──────────────────┤
│ CreateUser      │         │ GetUserById      │
│ UpdateUser      │         │ GetAllUsers      │
│ ChangePassword  │         │ GetByEmail       │
│ DeleteUser      │         │ GetModules       │
│ VerifyEmail     │         │ ValidateToken    │
└────────┬────────┘         └────────┬─────────┘
         │                           │
         ▼                           ▼
   CommandHandler              QueryHandler
         │                           │
         └──────────┬────────────────┘
                    ▼
              Domain Model
                    │
                    ▼
              PostgreSQL
```

### Beneficios

✅ **Separación de responsabilidades** - Código más mantenible  
✅ **Escalabilidad** - Escalar lectura/escritura independientemente  
✅ **Optimización** - Queries optimizadas para lectura  
✅ **Simplicidad** - Lógica de negocio clara  

## 📁 Estructura del Proyecto

```
user-service-cqrs/
├── src/main/java/com/saas/platform/user/
│   ├── application/              # Capa de aplicación
│   │   ├── commands/             # 🔴 COMMANDS (Write)
│   │   │   ├── UserCommands.java
│   │   │   ├── handlers/
│   │   │   │   └── UserCommandHandler.java
│   │   │   └── dto/
│   │   │       └── CommandResult.java
│   │   └── queries/              # 🔵 QUERIES (Read)
│   │       ├── UserQueries.java
│   │       ├── handlers/
│   │       │   └── UserQueryHandler.java
│   │       └── dto/
│   │           └── UserDTO.java
│   ├── domain/                   # Capa de dominio
│   │   ├── model/
│   │   │   ├── Usuario.java
│   │   │   └── Perfil.java
│   │   └── repository/
│   │       └── UsuarioRepository.java
│   └── infrastructure/           # Capa de infraestructura
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   └── CqrsConfig.java
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   └── JwtAuthenticationFilter.java
│       └── email/
│           └── EmailService.java
├── docker/
│   ├── Dockerfile
│   └── init-scripts/
└── docker-compose.yml
```

## 🐳 Docker - Inicio Rápido

### Opción 1: Con Docker Compose (Recomendado)

```bash
# 1. Clonar y configurar
git clone <repo>
cd user-service-cqrs

# 2. Configurar variables de entorno
cp .env.example .env
nano .env  # Editar EMAIL y JWT_SECRET

# 3. Levantar todos los servicios
docker-compose up -d

# 4. Ver logs
docker-compose logs -f user-service

# Servicios disponibles:
# - User Service: http://localhost:8082
# - PostgreSQL: localhost:5432
# - Redis: localhost:6379
# - pgAdmin: http://localhost:5050
```

### Opción 2: Solo User Service

```bash
# 1. Build imagen
docker build -t user-service-cqrs:latest .

# 2. Ejecutar contenedor
docker run -d \
  --name user-service \
  -p 8082:8082 \
  -e DB_HOST=host.docker.internal \
  -e JWT_SECRET=YourSecretKey \
  -e MAIL_USERNAME=your@gmail.com \
  -e MAIL_PASSWORD=app-password \
  user-service-cqrs:latest
```

## 🔧 Comandos Docker Útiles

```bash
# Ver servicios corriendo
docker-compose ps

# Parar servicios
docker-compose down

# Parar y eliminar volúmenes
docker-compose down -v

# Rebuild servicio
docker-compose up -d --build user-service

# Ver logs en tiempo real
docker-compose logs -f

# Ejecutar comandos dentro del contenedor
docker-compose exec user-service sh

# Reiniciar servicio
docker-compose restart user-service
```

## ⚙️ Configuración

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/saas_platform
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

jwt:
  secret: ${JWT_SECRET:change-this-secret}
  access-token-expiration: 3600000   # 1 hora
  refresh-token-expiration: 604800000 # 7 días

cqrs:
  async-commands: true  # Commands asíncronos
  event-store: false    # Event Sourcing deshabilitado
```

### Variables de Entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_HOST` | Host de PostgreSQL | localhost |
| `DB_PASSWORD` | Password de PostgreSQL | postgres |
| `JWT_SECRET` | Secret para JWT (256+ bits) | - |
| `MAIL_USERNAME` | Email SMTP | - |
| `MAIL_PASSWORD` | Password SMTP | - |
| `ASYNC_COMMANDS` | Comandos asíncronos | true |

## 📡 API Endpoints

### Commands (Write Operations)

```http
POST /api/commands/users/create
POST /api/commands/users/{id}/update
POST /api/commands/users/{id}/deactivate
POST /api/commands/users/{id}/delete
POST /api/commands/auth/login
POST /api/commands/auth/register
POST /api/commands/password/change
POST /api/commands/password/reset
POST /api/commands/email/verify
```

### Queries (Read Operations)

```http
GET /api/queries/users/{id}
GET /api/queries/users
GET /api/queries/users/email/{email}
GET /api/queries/users/{id}/modules?organizacionId=1
GET /api/queries/auth/validate?token=xxx
```

### Ejemplo: Crear Usuario (Command)

```bash
curl -X POST http://localhost:8082/api/commands/users/create \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!",
    "nombre": "Juan",
    "apellido": "Pérez"
  }'

Response:
{
  "success": true,
  "message": "Usuario creado exitosamente",
  "data": 1
}
```

### Ejemplo: Obtener Usuario (Query)

```bash
curl http://localhost:8082/api/queries/users/1

Response:
{
  "id": 1,
  "email": "user@example.com",
  "nombre": "Juan",
  "apellido": "Pérez",
  "activo": true,
  "emailVerificado": false
}
```

## 🔐 Autenticación JWT

```bash
# 1. Login
curl -X POST http://localhost:8082/api/commands/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}'

Response:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1
  }
}

# 2. Usar token en requests
curl http://localhost:8082/api/queries/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## 📊 Monitoreo

### Health Check

```bash
curl http://localhost:8082/actuator/health

{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Métricas Prometheus

```bash
curl http://localhost:8082/actuator/prometheus
```

## 🗄️ Base de Datos

### Inicializar Schema

```bash
# Los scripts SQL se ejecutan automáticamente al iniciar PostgreSQL
# Ubicación: docker/init-scripts/*.sql

# O manualmente:
docker-compose exec postgres psql -U postgres -d saas_platform -f /docker-entrypoint-initdb.d/01_create_database.sql
```

### Acceder a PostgreSQL

```bash
# Via Docker
docker-compose exec postgres psql -U postgres -d saas_platform

# Via pgAdmin
# URL: http://localhost:5050
# Email: admin@saas.com
# Password: admin
```

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests con Docker

```bash
# Levantar entorno de testing
docker-compose -f docker-compose.test.yml up -d

# Ejecutar tests
mvn verify

# Limpiar
docker-compose -f docker-compose.test.yml down -v
```

### Manual Testing

```bash
# Crear usuario
curl -X POST http://localhost:8082/api/commands/users/create \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!","nombre":"Test","apellido":"User"}'

# Verificar email (check your email for code)
curl -X POST http://localhost:8082/api/commands/email/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","code":"123456"}'

# Login
curl -X POST http://localhost:8082/api/commands/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!"}'
```

## 🔄 Flujo CQRS Completo

### Ejemplo: Actualizar Usuario

```
1. Frontend envía request:
   PUT /api/commands/users/1/update
   { "nombre": "Juan Carlos" }

2. Controller recibe request
   ↓
3. Crea UpdateUserCommand
   ↓
4. UserCommandHandler.handle(command)
   ↓
5. Valida y ejecuta lógica de negocio
   ↓
6. Guarda en PostgreSQL
   ↓
7. Retorna CommandResult
   ↓
8. Frontend recibe:
   {
     "success": true,
     "message": "Usuario actualizado"
   }

9. Frontend hace query para obtener datos:
   GET /api/queries/users/1
   ↓
10. UserQueryHandler.handle(query)
    ↓
11. Retorna UserDTO
```

## 🚀 Despliegue en Producción

### Build Optimizado

```bash
# Multi-stage build (imagen pequeña)
docker build -t user-service-cqrs:1.0.0 .

# Imagen resultante: ~200MB
```

### Kubernetes (ejemplo)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: user-service
        image: user-service-cqrs:1.0.0
        env:
        - name: DB_HOST
          value: postgres-service
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
```

## 📈 Escalabilidad

CQRS permite escalar independientemente:

```
┌─────────────┐
│  Load       │
│  Balancer   │
└──────┬──────┘
       │
   ┌───┴────┐
   │        │
┌──▼──┐  ┌──▼──┐  ┌──────┐
│ CMD │  │ CMD │  │ QUERY│  ← Más replicas para queries
│  1  │  │  2  │  │  1-N │
└──┬──┘  └──┬──┘  └───┬──┘
   │        │          │
   └────┬───┴──────────┘
        │
    ┌───▼────┐
    │   DB   │
    └────────┘
```

## 🔍 Troubleshooting

### Servicio no inicia

```bash
# Ver logs
docker-compose logs user-service

# Verificar BD
docker-compose exec postgres pg_isready

# Verificar variables
docker-compose exec user-service env | grep DB_
```

### Email no se envía

```bash
# Verificar configuración SMTP
docker-compose logs user-service | grep -i mail

# Test SMTP desde contenedor
docker-compose exec user-service sh
apk add --no-cache curl
curl -v telnet://smtp.gmail.com:587
```

## 📚 Recursos

- [Patrón CQRS](https://martinfowler.com/bliki/CQRS.html)
- [Docker Compose](https://docs.docker.com/compose/)
- [Spring Boot](https://spring.io/projects/spring-boot)

## 📄 Licencia

MIT License - Uso educativo
