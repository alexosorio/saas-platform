# 🔐 Administración de Roles y Módulos

## ✨ Funcionalidades Implementadas

### 1. **Gestión Completa de Roles**
- ✅ Crear roles con permisos personalizados
- ✅ Actualizar roles y permisos
- ✅ Eliminar roles (solo no-sistema)
- ✅ Listar todos los roles
- ✅ Asignar roles a usuarios

### 2. **Sistema de Permisos por Módulo**
- ✅ Permisos CRUD por módulo (Crear, Leer, Actualizar, Eliminar)
- ✅ Configuración granular de accesos
- ✅ Roles del sistema (no editables)
- ✅ Roles personalizados (editables)

### 3. **Carga de Módulos en Login**
- ✅ Al hacer login, se retornan los módulos accesibles
- ✅ Filtrado por rol del usuario
- ✅ Filtrado por plan de suscripción
- ✅ Permisos CRUD incluidos por módulo

## 📋 Nuevas Entidades

### Rol
```java
- id: Long
- nombre: String (único)
- descripcion: String
- esSistema: Boolean
- activo: Boolean
- permisos: Set<RolModulo>
```

###  RolModulo (Permisos)
```java
- id: Long
- rol: Rol
- moduloId: Long
- moduloCodigo: String
- moduloNombre: String
- crear: Boolean
- leer: Boolean
- actualizar: Boolean
- eliminar: Boolean
```

### Usuario (modificado)
```java
+ organizacionId: Long
+ rol: Rol
```

## 🔌 Nuevos Endpoints

### Administración de Roles

```http
# Crear rol
POST /api/roles
Content-Type: application/json
Authorization: Bearer {token}

{
  "nombre": "Editor",
  "descripcion": "Puede editar contenido",
  "permisos": [
    {
      "moduloId": 1,
      "moduloCodigo": "INVENTARIO",
      "moduloNombre": "Gestión de Inventario",
      "crear": true,
      "leer": true,
      "actualizar": true,
      "eliminar": false
    }
  ]
}

# Listar roles
GET /api/roles
Authorization: Bearer {token}

# Obtener rol por ID
GET /api/roles/{id}
Authorization: Bearer {token}

# Actualizar rol
PUT /api/roles/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
  "nombre": "Editor Senior",
  "descripcion": "Puede editar y eliminar",
  "permisos": [...]
}

# Eliminar rol
DELETE /api/roles/{id}
Authorization: Bearer {token}

# Asignar rol a usuario
POST /api/users/{userId}/assign-role
Content-Type: application/json
Authorization: Bearer {token}

{
  "rolId": 2
}
```

## 🔐 Login con Módulos

### Request
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@demo.com",
  "password": "Admin123!"
}
```

### Response (Modificado - INCLUYE MÓDULOS)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "userId": 1,
  "email": "admin@demo.com",
  "emailVerificado": true,
  "rol": {
    "id": 1,
    "nombre": "Administrador",
    "permisos": [
      {
        "moduloId": 1,
        "moduloCodigo": "INVENTARIO",
        "moduloNombre": "Gestión de Inventario",
        "crear": true,
        "leer": true,
        "actualizar": true,
        "eliminar": true
      },
      {
        "moduloId": 2,
        "moduloCodigo": "VENTAS",
        "moduloNombre": "Punto de Venta",
        "crear": true,
        "leer": true,
        "actualizar": true,
        "eliminar": true
      }
    ]
  },
  "modulosAccesibles": [
    {
      "id": 1,
      "nombre": "Gestión de Inventario",
      "codigo": "INVENTARIO",
      "ruta": "/inventario",
      "icono": "inventory",
      "crear": true,
      "leer": true,
      "actualizar": true,
      "eliminar": true
    },
    {
      "id": 2,
      "nombre": "Punto de Venta",
      "codigo": "VENTAS",
      "ruta": "/ventas",
      "icono": "point_of_sale",
      "crear": true,
      "leer": true,
      "actualizar": true,
      "eliminar": true
    }
  ]
}
```

## 📊 Ejemplo de Uso Completo

### 1. Crear Roles Iniciales

```bash
# Rol Administrador (permisos completos)
curl -X POST http://localhost:8082/api/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre": "Administrador",
    "descripcion": "Acceso completo al sistema",
    "permisos": [
      {
        "moduloId": 1,
        "moduloCodigo": "INVENTARIO",
        "moduloNombre": "Inventario",
        "crear": true, "leer": true, "actualizar": true, "eliminar": true
      },
      {
        "moduloId": 2,
        "moduloCodigo": "VENTAS",
        "moduloNombre": "Ventas",
        "crear": true, "leer": true, "actualizar": true, "eliminar": true
      }
    ]
  }'

# Rol Vendedor (solo lectura y creación)
curl -X POST http://localhost:8082/api/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre": "Vendedor",
    "descripcion": "Puede registrar ventas",
    "permisos": [
      {
        "moduloId": 1,
        "moduloCodigo": "INVENTARIO",
        "moduloNombre": "Inventario",
        "crear": false, "leer": true, "actualizar": false, "eliminar": false
      },
      {
        "moduloId": 2,
        "moduloCodigo": "VENTAS",
        "moduloNombre": "Ventas",
        "crear": true, "leer": true, "actualizar": false, "eliminar": false
      }
    ]
  }'
```

### 2. Asignar Rol a Usuario

```bash
curl -X POST http://localhost:8082/api/users/5/assign-role \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"rolId": 2}'
```

### 3. Login y Recibir Módulos

```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "vendedor@empresa.com",
    "password": "Password123!"
  }'

# Response incluye modulosAccesibles con permisos
```

## 🔄 Flujo Completo

```
1. Usuario hace login
   ↓
2. AuthService verifica credenciales
   ↓
3. Carga rol del usuario desde BD
   ↓
4. Obtiene permisos del rol (RolModulo)
   ↓
5. Construye lista de módulos accesibles
   ↓
6. Retorna JWT + módulos + permisos
   ↓
7. Frontend almacena permisos
   ↓
8. Frontend muestra solo módulos permitidos
   ↓
9. Frontend habilita/deshabilita botones según CRUD
```

## 🎯 Casos de Uso

### Frontend: Mostrar Menú Dinámico
```javascript
// En el frontend después del login
const response = await login(email, password);
const modulosPermitidos = response.modulosAccesibles;

// Mostrar solo módulos con permiso de lectura
const menu = modulosPermitidos.filter(m => m.leer);

// Habilitar botón "Crear" solo si tiene permiso
if (moduloActual.crear) {
  showCreateButton();
}
```

### Backend: Validar Permisos
```java
// En cada endpoint sensible
@PreAuthorize("hasAuthority('INVENTARIO:CREAR')")
@PostMapping("/productos")
public ResponseEntity<?> crearProducto(...) {
    // ...
}
```

## 📝 Tablas de Base de Datos

```sql
-- Roles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255),
    es_sistema BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Permisos de roles sobre módulos
CREATE TABLE roles_modulos (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL REFERENCES roles(id),
    modulo_id BIGINT NOT NULL,
    modulo_codigo VARCHAR(50),
    modulo_nombre VARCHAR(100),
    crear BOOLEAN DEFAULT FALSE,
    leer BOOLEAN DEFAULT FALSE,
    actualizar BOOLEAN DEFAULT FALSE,
    eliminar BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

-- Usuarios (modificado)
ALTER TABLE usuarios ADD COLUMN organizacion_id BIGINT;
ALTER TABLE usuarios ADD COLUMN rol_id BIGINT REFERENCES roles(id);
```

## ✅ Listo para Usar

El sistema está completamente implementado y listo para:
1. Crear roles personalizados
2. Asignar permisos granulares
3. Login con carga automática de módulos
4. Control de acceso basado en roles (RBAC)
