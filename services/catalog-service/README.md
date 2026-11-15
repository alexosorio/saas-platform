# Catalog Service

Microservicio de catálogo para la plataforma SaaS: gestión de **productos**, **unidades** (con IMEI y características dinámicas), **inventario** por bodega, **clientes**, **listas de precios** (minorista / mayorista) y **órdenes de venta** con pagos.

Se integra con el `user-service` existente (puerto 8082) compartiendo la **misma base de datos PostgreSQL** (schema `gets_shared`) y el **mismo secreto JWT**.

## Stack
- Java 17, Spring Boot 3.2, Spring Security 6, Spring Data JPA
- PostgreSQL (schema `gets_shared`), Hibernate 6 con tipos ENUM nativos de PostgreSQL
- JWT (jjwt 0.12.3) — valida los tokens emitidos por el user-service
- Puerto: **8083**

## Integración con el user-service (importante)

El `user-service` emite el access token con los claims `userId`, `email`, `emailVerificado` (subject = userId). Este servicio:

1. **Valida el token** con el mismo `JWT_SECRET`. Por eso **ambos servicios deben usar el mismo valor de `JWT_SECRET`**.
2. Extrae `userId` y `email` del token. El token no incluye roles ni `organizacionId`, por lo que:
   - La autorización es por *token válido* (cualquier usuario autenticado). No hay restricciones por rol.
   - `organizacionId` se asume `1` (modo single-tenant). Si más adelante el user-service incluye `organizacionId` en el JWT, este servicio lo tomará automáticamente.

Flujo: el frontend hace login contra el user-service (8082), guarda el `accessToken` en `localStorage` y lo reenvía a este servicio (8083) en el header `Authorization: Bearer <token>`.

## Base de datos

La base de datos **ya existe** (schema `gets_shared` con todas sus tablas y tipos ENUM). Por eso Hibernate está configurado con `ddl-auto: validate`: **nunca crea ni altera tablas**, solo valida que el mapeo de entidades coincida con el esquema real.

Tablas gestionadas: `categorias_productos`, `marcas`, `productos`, `caracteristicas_productos`, `unidades`, `valores_caracteristicas`, `bodegas`, `inventario`, `movimientos_inventario`, `listas_precios`, `precios`, `clientes`, `ordenes`, `detalles_orden`, `pagos_orden`.

## Configuración

Variables de entorno (ver `.env.example`):

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_HOST` | Host de PostgreSQL | localhost |
| `DB_PORT` | Puerto | 5432 |
| `DB_NAME` | Nombre de la BD | gets_db |
| `DB_USER` / `DB_PASSWORD` | Credenciales | postgres / postgres |
| `JWT_SECRET` | **Debe coincidir con el del user-service** | (ver .env.example) |
| `SERVER_PORT` | Puerto del servicio | 8083 |

## Ejecutar

### Local (Maven)
```bash
mvn clean package
java -jar target/catalog-service-1.0.0.jar
```

### Docker
```bash
cp .env.example .env   # ajustar DB y JWT_SECRET
docker-compose up -d --build
```

> Nota: si compilas en un entorno sin acceso a Maven Central, descarga primero las dependencias en una máquina con acceso (`mvn -o` para offline después).

## Endpoints principales

Todos requieren `Authorization: Bearer <token>` salvo `/actuator/**`.

| Recurso | Base | Operaciones |
|---------|------|-------------|
| Categorías | `/api/categories` | CRUD, `/active`, `/{id}/activate`, `/{id}/deactivate` |
| Marcas | `/api/brands` | CRUD, `/active`, activate/deactivate |
| Productos | `/api/products` | CRUD, `/active`, `/search?q=`, activate/deactivate |
| Características | `/api/characteristics` | CRUD, `/by-type/{aplicableA}` |
| Unidades | `/api/units` | `/product/{id}`, `/{id}`, `/imei/{imei}`, POST, DELETE, `/{id}/state/{estado}` |
| Bodegas | `/api/warehouses` | CRUD, `/by-sede/{id}` |
| Inventario | `/api/inventory` | `/warehouse/{id}`, `/product/{id}`, `/product/{id}/stock`, POST `/adjust`, `/movements/{id}` |
| Clientes | `/api/clients` | CRUD |
| Listas de precios | `/api/lists` | CRUD, `/active`, activate/deactivate, `/{id}/prices` (GET/POST), `/prices/{id}` (DELETE) |
| Órdenes | `/api/orders` | GET, `/by-state/{estado}`, `/{id}`, POST, `/{id}/confirm`, `/{id}/status`, `/{id}/cancel`, `/{id}/payments` (GET/POST) |

## Lógica de negocio destacada

- **Productos sin precio propio**: el precio de venta vive en `precios`, ligado a una `lista_precios` (minorista/mayorista). Un mismo producto puede tener precios distintos por lista.
- **Unidades**: cada unidad física tiene código único, IMEI opcional, condición (nuevo/usado), estado (`disponible`, `vendido`, `reservado`, `dañado`, `en_reparacion`) y características dinámicas (modelo EAV vía `valores_caracteristicas`).
- **Órdenes**: se crean en `borrador`. Al **confirmar** se descuenta inventario (movimientos tipo `salida`) y las unidades pasan a `vendido`. Al **cancelar** una orden confirmada se reingresa stock con movimientos tipo `entrada` (el esquema no define `devolucion`) y las unidades vuelven a `disponible`.
- **Pagos**: parciales o totales; al saldar el total una orden confirmada pasa a `completada`.
