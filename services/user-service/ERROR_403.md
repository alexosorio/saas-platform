# 🔒 Solución: Error 403 Forbidden

## ❌ Error
```
HTTP/1.1 403 Forbidden
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Response body is empty
```

## 🔍 Causas Comunes

### 1. CSRF Protection (Más común en Spring Security)
### 2. CORS mal configurado
### 3. Endpoint no está en lista de permitidos
### 4. JWT Filter rechazando requests públicos

## ✅ Solución Aplicada

### SecurityConfig Corregido

#### Cambios Principales:

**1. CORS Mejorado**
```java
// ❌ ANTES (podía causar problemas)
configuration.setAllowedOrigins(List.of("*"));
configuration.setAllowCredentials(false);

// ✅ AHORA (funciona con credenciales)
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowCredentials(true);
configuration.setMaxAge(3600L); // Cache preflight
```

**2. Endpoints Públicos Explícitos**
```java
// ❌ ANTES (demasiado genérico)
.requestMatchers("/api/auth/**").permitAll()

// ✅ AHORA (específico y claro)
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers("/api/auth/register").permitAll()
.requestMatchers("/api/auth/refresh").permitAll()
```

**3. Manejo de Errores Mejorado**
```java
// Ahora retorna JSON con detalles del error
.exceptionHandling(ex -> {
    ex.authenticationEntryPoint(...) // 401
    ex.accessDeniedHandler(...)      // 403
});
```

**4. Logging para Debug**
```java
@Slf4j
public class SecurityConfig {
    // Logs en cada configuración para debugging
}
```

## 🧪 Cómo Probar

### 1. Endpoints Públicos (SIN autenticación)

```bash
# ✅ Login
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"Admin123!"}'

# ✅ Register
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"test@test.com",
    "password":"Test123!",
    "nombre":"Test",
    "apellido":"User"
  }'

# ✅ Health check
curl http://localhost:8082/actuator/health

# ✅ Verify email
curl -X POST http://localhost:8082/api/users/verify-email \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","code":"123456"}'

# ✅ Forgot password
curl -X POST http://localhost:8082/api/users/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com"}'
```

### 2. Endpoints Protegidos (CON autenticación)

```bash
# 1. Primero hacer login y obtener token
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"Admin123!"}' \
  | jq -r '.accessToken')

echo "Token: $TOKEN"

# 2. Usar token en request protegido
curl -X GET http://localhost:8082/api/users/me \
  -H "Authorization: Bearer $TOKEN"

# 3. Obtener usuario por ID
curl -X GET http://localhost:8082/api/users/1 \
  -H "Authorization: Bearer $TOKEN"

# 4. Actualizar usuario
curl -X PUT http://localhost:8082/api/users/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan","apellido":"Perez"}'
```

## 🔍 Debugging

### Ver Logs de Spring Security

```bash
# Ver logs del servicio
docker-compose logs -f user-service | grep -i security

# Ver logs de requests
docker-compose logs -f user-service | grep -i "http-nio"

# Ver logs de autenticación
docker-compose logs -f user-service | grep -i "authentication"
```

### Habilitar Logging Detallado

```yaml
# application.yml
logging:
  level:
    org.springframework.security: DEBUG
    com.saas.platform.user.security: DEBUG
    org.springframework.web: DEBUG
```

### Verificar Configuración Actual

```bash
# Ver variables de entorno
docker-compose exec user-service env | grep -E "JWT|MAIL|DB"

# Verificar que el servicio está corriendo
curl http://localhost:8082/actuator/health

# Ver info del servicio
curl http://localhost:8082/actuator/info
```

## 🚨 Errores Específicos y Soluciones

### Error: "403 en /api/auth/login"

**Causa:** CORS preflight failing o CSRF habilitado

**Solución:**
```java
// SecurityConfig.java
.csrf(AbstractHttpConfigurer::disable)  // ✅ Deshabilitado
.cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ✅ CORS activo
```

**Test:**
```bash
# Verificar que CORS funciona
curl -X OPTIONS http://localhost:8082/api/auth/login \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -v

# Debe retornar headers CORS:
# Access-Control-Allow-Origin: *
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```

### Error: "403 en endpoints públicos"

**Causa:** Endpoint no está en `.permitAll()`

**Solución:** Verificar que el endpoint está en la lista:
```java
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers("/api/auth/register").permitAll()
// etc...
```

**Test:**
```bash
# Ver logs cuando haces el request
docker-compose logs -f user-service

# Buscar líneas como:
# "Configurando reglas de autorización"
# "Request matched pattern: /api/auth/login"
```

### Error: "403 con token válido"

**Causa:** JwtAuthenticationFilter rechazando el request

**Solución:** Verificar que el token es válido
```bash
# Decodificar JWT (sin verificar firma)
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq

# Verificar que:
# - "exp" no ha expirado
# - "sub" contiene el email
# - "userId" existe
```

### Error: "403 desde Postman/Browser"

**Causa:** CORS preflight

**Solución:**
```java
// SecurityConfig.java - CORS debe permitir:
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
```

**Test en Postman:**
1. Deshabilitar "SSL certificate verification"
2. Headers:
   - `Content-Type: application/json`
   - `Accept: application/json`
3. Para endpoints protegidos:
   - `Authorization: Bearer {token}`

## 🔧 Configuración Recomendada

### Para Desarrollo Local

```yaml
# application.yml
spring:
  security:
    # Logging detallado
    debug: true

logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.web.cors: DEBUG
```

```java
// SecurityConfig.java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))

// CORS muy permisivo para desarrollo
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowedMethods(List.of("*"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
```

### Para Producción

```yaml
# application-prod.yml
logging:
  level:
    org.springframework.security: WARN
    org.springframework.web.cors: WARN
```

```java
// SecurityConfig.java - CORS restrictivo
configuration.setAllowedOriginPatterns(List.of(
    "https://tuapp.com",
    "https://www.tuapp.com"
));
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
```

## 📋 Checklist de Verificación

- [ ] CSRF deshabilitado (`.csrf(AbstractHttpConfigurer::disable)`)
- [ ] CORS configurado correctamente
- [ ] Endpoints públicos en `.permitAll()`
- [ ] JWT Filter no bloquea endpoints públicos
- [ ] Token válido para endpoints protegidos
- [ ] Headers correctos en requests
- [ ] Logs de seguridad revisados

## 🎯 Endpoints por Tipo

### Públicos (NO requieren token)
```
POST   /api/auth/login
POST   /api/auth/register
POST   /api/auth/refresh
POST   /api/users/verify-email
POST   /api/users/resend-verification
POST   /api/users/forgot-password
POST   /api/users/request-password-reset
POST   /api/users/reset-password-code
POST   /api/users/reset-password-token
GET    /actuator/health
GET    /actuator/info
```

### Protegidos (Requieren token)
```
GET    /api/users/me
GET    /api/users/{id}
PUT    /api/users/{id}
POST   /api/users/change-password
DELETE /api/users/{id}
GET    /api/users
POST   /api/users
PUT    /api/users/{id}/activate
PUT    /api/users/{id}/deactivate
```

## 🔄 Flujo Correcto

### 1. Request Público
```
Cliente → Request → CORS Preflight (OPTIONS) → SecurityFilterChain
                                                     ↓
                                              .permitAll()
                                                     ↓
                                              Controller
```

### 2. Request Protegido
```
Cliente → Request + Token → CORS → SecurityFilterChain
                                          ↓
                                    JwtAuthenticationFilter
                                          ↓
                                    Validar JWT
                                          ↓
                                    .authenticated()
                                          ↓
                                    Controller
```

## 💡 Tips Finales

1. **Siempre revisar logs primero**
   ```bash
   docker-compose logs -f user-service
   ```

2. **Usar curl para debugging**
   ```bash
   curl -v http://localhost:8082/api/auth/login
   # -v muestra headers completos
   ```

3. **Verificar que el servicio está corriendo**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

4. **Reiniciar después de cambios**
   ```bash
   docker-compose restart user-service
   ```

5. **Limpiar y reconstruir si es necesario**
   ```bash
   docker-compose down
   mvn clean package
   docker-compose up -d --build
   ```

---

**¿Sigue sin funcionar?** Comparte:
1. El endpoint exacto que estás llamando
2. El comando curl completo
3. Los logs del servicio
