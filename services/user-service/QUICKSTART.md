# 🚀 User Service CQRS - Inicio Rápido

## ✅ Archivos Corregidos

Todos los Commands y Queries ahora están en archivos individuales como requiere Java.

## 📁 Estructura

```
src/main/java/com/saas/platform/user/
├── application/
│   ├── commands/               # ✅ Archivos individuales
│   │   ├── CreateUserCommand.java
│   │   ├── UpdateUserCommand.java
│   │   ├── LoginCommand.java
│   │   ├── ... (13 commands)
│   │   ├── handlers/
│   │   │   └── UserCommandHandler.java
│   │   └── dto/
│   │       └── CommandResult.java
│   └── queries/                # ✅ Archivos individuales
│       ├── GetUserByIdQuery.java
│       ├── GetAllUsersQuery.java
│       ├── ... (7 queries)
│       └── handlers/
│           └── (pendiente)
```

## 🐳 Docker - Un Solo Comando

```bash
# 1. Configurar email
cp .env.example .env
nano .env  # Editar MAIL_USERNAME y MAIL_PASSWORD

# 2. Levantar todo
docker-compose up -d

# ✅ Listo! Servicio en http://localhost:8082
```

## 🔧 Sin Docker

```bash
# 1. Base de datos
# Ejecutar scripts SQL de saas-platform-postgres/

# 2. Configurar
nano src/main/resources/application.yml

# 3. Compilar
mvn clean package

# 4. Ejecutar
java -jar target/user-service-cqrs.jar
```

## 📡 Probar API

```bash
# Health check
curl http://localhost:8082/actuator/health

# Crear usuario (Command)
curl -X POST http://localhost:8082/api/commands/users/create \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!","nombre":"Test","apellido":"User"}'

# Obtener usuario (Query)
curl http://localhost:8082/api/queries/users/1
```

## 📚 Documentación Completa

Ver `README.md` para toda la documentación de CQRS, Docker y API.
