# 🔐 Autenticación - Cuándo Usar Bearer Token

## 📋 Resumen Rápido

| Tipo de Endpoint | Requiere Token | Ejemplo |
|------------------|----------------|---------|
| **Públicos** | ❌ NO | Login, Register, Verify Email |
| **Protegidos** | ✅ SÍ | Get User, Update User, Delete |
| **Admin** | ✅ SÍ + Rol Admin | Create User, Delete User |

## 🔓 Endpoints PÚBLICOS (SIN Bearer Token)

Estos endpoints **NO requieren autenticación**:

### Autenticación
```bash
POST /api/auth/login           # ❌ No token
POST /api/auth/register        # ❌ No token
POST /api/auth/refresh         # ❌ No token (usa refresh token en body)
```

### Verificación de Email
```bash
POST /api/users/verify-email           # ❌ No token
POST /api/users/resend-verification    # ❌ No token
```

### Recuperación de Contraseña
```bash
POST /api/users/forgot-password        # ❌ No token
POST /api/users/request-password-reset # ❌ No token
POST /api/users/reset-password-code    # ❌ No token
POST /api/users/reset-password-token   # ❌ No token
```

### Monitoreo
```bash
GET /actuator/health                   # ❌ No token
GET /actuator/info                     # ❌ No token
```

## 🔒 Endpoints PROTEGIDOS (SÍ Bearer Token)

Estos endpoints **REQUIEREN autenticación**:

### Usuario Actual
```bash
GET /api/users/me                      # ✅ Token requerido
```

### Gestión de Usuario
```bash
GET    /api/users/{id}                 # ✅ Token requerido
PUT    /api/users/{id}                 # ✅ Token requerido
POST   /api/users/change-password      # ✅ Token requerido
GET    /api/users/{id}/modules         # ✅ Token requerido
```

### Admin Only (Token + Rol Admin)
```bash
POST   /api/users                      # ✅ Token + Admin
GET    /api/users                      # ✅ Token + Admin
DELETE /api/users/{id}                 # ✅ Token + Admin
PUT    /api/users/{id}/activate        # ✅ Token + Admin
PUT    /api/users/{id}/deactivate      # ✅ Token + Admin
```

## 🎯 Cómo Obtener el Bearer Token

### 1. Login (Obtener Token)

```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "Admin123!"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "userId": 1,
  "email": "admin@demo.com",
  "emailVerificado": true
}
```

### 2. Usar el Token

```bash
# Guardar token en variable
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Usar en requests
curl -X GET http://localhost:8082/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

## 📝 Ejemplos Completos

### Ejemplo 1: Flujo Completo de Registro

```bash
# 1. Registro (NO requiere token)
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nuevo@usuario.com",
    "password": "Password123!",
    "nombre": "Juan",
    "apellido": "Pérez"
  }'

# Response:
# {
#   "message": "Usuario registrado. Verifica tu email.",
#   "success": true
# }

# 2. Verificar email (NO requiere token)
# (Revisa tu email y obtén el código)
curl -X POST http://localhost:8082/api/users/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nuevo@usuario.com",
    "code": "123456"
  }'

# 3. Login (NO requiere token)
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nuevo@usuario.com",
    "password": "Password123!"
  }'

# Response:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIs...",
#   "userId": 5
# }

# 4. Obtener perfil (SÍ requiere token)
TOKEN="eyJhbGciOiJIUzI1NiIs..."

curl -X GET http://localhost:8082/api/users/me \
  -H "Authorization: Bearer $TOKEN"

# Response:
# {
#   "id": 5,
#   "email": "nuevo@usuario.com",
#   "nombre": "Juan",
#   "apellido": "Pérez"
# }
```

### Ejemplo 2: Actualizar Perfil

```bash
# Primero login
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"Admin123!"}' \
  | jq -r '.accessToken')

# Actualizar perfil (requiere token)
curl -X PUT http://localhost:8082/api/users/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Carlos",
    "apellido": "Pérez López",
    "telefono": "3001234567"
  }'
```

### Ejemplo 3: Cambiar Contraseña

```bash
# Login y obtener token
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"Admin123!"}' \
  | jq -r '.accessToken')

# Cambiar contraseña (requiere token)
curl -X POST http://localhost:8082/api/users/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "NewPassword123!"
  }'
```

## 🔄 Renovar Token (Refresh Token)

```bash
# El access token expira en 1 hora
# Usar refresh token para obtener nuevo access token

curl -X POST http://localhost:8082/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'

# Response:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIs...",  // Nuevo token
#   "refreshToken": "eyJhbGciOiJIUzI1NiIs...", // Nuevo refresh token
#   "tokenType": "Bearer",
#   "expiresIn": 3600000
# }
```

## 🌐 Comunicación entre Microservicios

### Escenario: User Service llama Authorization Service

#### Opción 1: Service Account Token (Recomendado)

```java
@Service
public class ModuleLoadingService {
    
    @Value("${services.authorization.url}")
    private String authServiceUrl;
    
    @Value("${services.authorization.token}")
    private String serviceToken;  // Token de servicio
    
    private final RestTemplate restTemplate;
    
    public List<ModuloDTO> obtenerModulos(Long userId, Long organizacionId) {
        String url = authServiceUrl + "/api/modules?userId=" + userId 
                     + "&organizacionId=" + organizacionId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceToken);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<List<ModuloDTO>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<ModuloDTO>>() {}
        );
        
        return response.getBody();
    }
}
```

```yaml
# application.yml
services:
  authorization:
    url: http://localhost:8084
    token: ${SERVICE_ACCOUNT_TOKEN}  # Token especial para microservicios
```

#### Opción 2: Propagación de Token de Usuario

```java
@Service
public class ModuleLoadingService {
    
    public List<ModuloDTO> obtenerModulos(Long userId, Long organizacionId, String userToken) {
        String url = authServiceUrl + "/api/modules?userId=" + userId 
                     + "&organizacionId=" + organizacionId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userToken);  // Token del usuario
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<List<ModuloDTO>> response = restTemplate.exchange(
            url, HttpMethod.GET, entity,
            new ParameterizedTypeReference<List<ModuloDTO>>() {}
        );
        
        return response.getBody();
    }
}
```

#### Opción 3: Service Mesh (Istio, Linkerd)

```yaml
# No requiere código
# El service mesh maneja autenticación automáticamente
# Configuración en Kubernetes/Istio
```

## 🔑 Configuración de Service Account

### 1. Crear Token Especial para Microservicios

```java
@Service
public class ServiceAccountService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    public String generarTokenServicio(String serviceName) {
        return Jwts.builder()
            .setSubject("service:" + serviceName)
            .claim("type", "service")
            .claim("service", serviceName)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)) // 1 año
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
}
```

### 2. Validar Token de Servicio

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        
        String token = extractToken(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaims(token);
            
            if ("service".equals(claims.get("type"))) {
                // Es un token de servicio
                String serviceName = claims.get("service", String.class);
                // Dar permisos de servicio
                
            } else {
                // Es un token de usuario normal
                Long userId = claims.get("userId", Long.class);
                // Autenticar usuario
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 3. Configurar en application.yml

```yaml
services:
  authorization:
    url: http://authorization-service:8084
    token: ${SERVICE_ACCOUNT_TOKEN}

# En .env o secrets
SERVICE_ACCOUNT_TOKEN=eyJhbGciOiJIUzI1NiIs...
```

## 📱 Uso desde Frontend

### JavaScript/React

```javascript
// 1. Login y guardar token
const login = async (email, password) => {
  const response = await fetch('http://localhost:8082/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });
  
  const data = await response.json();
  
  // Guardar tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  
  return data;
};

// 2. Usar token en requests
const getUserProfile = async () => {
  const token = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8082/api/users/me', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  
  return response.json();
};

// 3. Interceptor para agregar token automáticamente (con Axios)
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8082',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Ahora todos los requests llevan el token automáticamente
const profile = await api.get('/api/users/me');
const updateProfile = await api.put('/api/users/1', { nombre: 'Juan' });
```

### Angular

```typescript
// auth.interceptor.ts
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('accessToken');
    
    if (token) {
      const cloned = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
      return next.handle(cloned);
    }
    
    return next.handle(req);
  }
}

// app.module.ts
import { HTTP_INTERCEPTORS } from '@angular/common/http';

providers: [
  {
    provide: HTTP_INTERCEPTORS,
    useClass: AuthInterceptor,
    multi: true
  }
]
```

## 🚨 Errores Comunes

### Error 401: Unauthorized

```bash
# Causa 1: Token no enviado
❌ curl http://localhost:8082/api/users/me

✅ curl http://localhost:8082/api/users/me \
     -H "Authorization: Bearer $TOKEN"

# Causa 2: Token expirado
✅ Usar refresh token para obtener nuevo access token

# Causa 3: Token inválido
✅ Hacer login de nuevo
```

### Error 403: Forbidden

```bash
# Causa: Token válido pero sin permisos
# Ejemplo: Usuario normal intentando endpoint de admin

❌ DELETE /api/users/5  # Requiere rol Admin

✅ Verificar que el usuario tenga el rol correcto
```

## 📋 Checklist de Autenticación

### Para Endpoints Públicos
- [ ] NO enviar header Authorization
- [ ] Solo Content-Type: application/json

### Para Endpoints Protegidos
- [ ] Hacer login primero
- [ ] Guardar accessToken
- [ ] Enviar header: `Authorization: Bearer {token}`
- [ ] Renovar token cuando expire (cada 1 hora)

### Para Comunicación entre Microservicios
- [ ] Generar service account token
- [ ] Configurar en variables de entorno
- [ ] Enviar en todas las llamadas inter-servicio
- [ ] O usar service mesh (Istio/Linkerd)

## 🎯 Resumen

| Escenario | Token Necesario |
|-----------|-----------------|
| Login, Register | ❌ NO |
| Verify Email | ❌ NO |
| Forgot Password | ❌ NO |
| Get User Profile | ✅ SÍ |
| Update User | ✅ SÍ |
| Admin Operations | ✅ SÍ + Admin Role |
| Microservice → Microservice | ✅ Service Account Token |

---

**Regla Simple:** Si el endpoint modifica datos o accede a información privada → **Requiere Bearer Token**
