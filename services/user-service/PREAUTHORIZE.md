# 🔐 Guía de @PreAuthorize - Spring Security

## ✅ Problema Resuelto

El error `Failed to evaluate expression 'hasRole('ADMIN') or #id == authentication.principal.id'` ocurría porque:

❌ **Antes**: `authentication.principal` era solo un String (userId)  
✅ **Ahora**: `authentication.principal` es un objeto `UserPrincipal` con todas las propiedades

## 📋 UserPrincipal Creado

```java
@Data
public class UserPrincipal implements UserDetails {
    private Long id;
    private String email;
    private String password;
    private boolean activo;
    private boolean emailVerificado;
    private List<String> roles;
    
    // Métodos de UserDetails...
}
```

## 🎯 Cómo Usar @PreAuthorize

### 1. Verificar Roles

```java
// Usuario debe tener rol ADMIN
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    // ...
}

// Usuario debe tener rol ADMIN o MANAGER
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@PutMapping("/{id}/activate")
public ResponseEntity<?> activateUser(@PathVariable Long id) {
    // ...
}
```

### 2. Verificar que el Usuario es Propietario

```java
// Solo el usuario puede ver su propio perfil (o admin)
@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
@GetMapping("/{id}")
public ResponseEntity<?> getUserById(@PathVariable Long id) {
    // ...
}

// Solo el usuario puede actualizar su propio perfil (o admin)
@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
@PutMapping("/{id}")
public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
    // ...
}
```

### 3. Verificar Email

```java
// Solo el usuario con ese email puede ejecutar la acción
@PreAuthorize("#email == authentication.principal.email")
@PostMapping("/change-password")
public ResponseEntity<?> changePassword(@RequestParam String email, @RequestBody ChangePasswordRequest request) {
    // ...
}
```

### 4. Combinaciones Complejas

```java
// Admin O (usuario activo Y email verificado)
@PreAuthorize("hasRole('ADMIN') or (authentication.principal.activo and authentication.principal.emailVerificado)")
@GetMapping("/sensitive-data")
public ResponseEntity<?> getSensitiveData() {
    // ...
}

// Admin Y email verificado
@PreAuthorize("hasRole('ADMIN') and authentication.principal.emailVerificado")
@PostMapping("/critical-action")
public ResponseEntity<?> criticalAction() {
    // ...
}
```

### 5. Usar en Service Layer

```java
@Service
public class UserService {
    
    // Solo admin puede ejecutar este método
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAllUsers() {
        // ...
    }
    
    // Usuario debe ser propietario o admin
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public UserResponse getUserProfile(Long userId) {
        // ...
    }
}
```

## 📖 Expresiones Disponibles

### Roles
```java
hasRole('ADMIN')                    // Tiene rol ADMIN
hasAnyRole('ADMIN', 'MANAGER')      // Tiene ADMIN o MANAGER
!hasRole('GUEST')                   // NO tiene rol GUEST
```

### Autenticación
```java
isAuthenticated()                   // Usuario autenticado
isAnonymous()                       // Usuario NO autenticado
!isAnonymous()                      // Usuario autenticado (equivalente a isAuthenticated)
```

### Principal (Usuario Actual)
```java
authentication.principal.id                     // ID del usuario
authentication.principal.email                  // Email del usuario
authentication.principal.activo                 // Usuario activo
authentication.principal.emailVerificado        // Email verificado
authentication.principal.roles                  // Lista de roles
```

### Parámetros del Método
```java
#id                                 // Parámetro del método
#userId                             // Parámetro del método
#request.email                      // Propiedad del objeto request
```

### Operadores
```java
and                                 // Y lógico
or                                  // O lógico
!                                   // Negación
==                                  // Igual
!=                                  // Diferente
>                                   // Mayor que
<                                   // Menor que
```

## 🎯 Ejemplos Completos

### UserController Actualizado

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    // Público - no requiere autenticación
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // ...
    }
    
    // Autenticado - cualquier usuario logueado
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        // ...
    }
    
    // Solo el usuario propietario o admin
    @PreAuthorize("hasRole('ADMINISTRADOR') or #id == authentication.principal.id")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        // ...
    }
    
    // Solo el usuario propietario o admin
    @PreAuthorize("hasRole('ADMINISTRADOR') or #id == authentication.principal.id")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
        @PathVariable Long id, 
        @RequestBody UpdateUserRequest request
    ) {
        // ...
    }
    
    // Solo admin
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        // ...
    }
    
    // Solo admin
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        // ...
    }
    
    // Solo admin
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        // ...
    }
    
    // Solo admin
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        // ...
    }
}
```

## 🔍 Obtener Usuario Actual en el Controller

```java
@GetMapping("/profile")
public ResponseEntity<?> getProfile(Authentication authentication) {
    UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
    
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "email", user.getEmail(),
        "roles", user.getRoles()
    ));
}

// O usando @AuthenticationPrincipal
@GetMapping("/profile")
public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "email", user.getEmail(),
        "roles", user.getRoles()
    ));
}
```

## 🚨 Errores Comunes y Soluciones

### Error 1: "Failed to evaluate expression"
```
❌ Problema: Principal no tiene la propiedad
✅ Solución: Usar UserPrincipal (ya implementado)
```

### Error 2: "Access Denied" cuando debería funcionar
```
❌ Problema: Rol mal escrito o falta "ROLE_" prefix
✅ Solución: 
   - En BD: almacenar "ADMINISTRADOR" (sin ROLE_)
   - En @PreAuthorize: usar hasRole('ADMINISTRADOR')
   - Spring agregará "ROLE_" automáticamente
```

### Error 3: "Cannot pass null or empty values"
```
❌ Problema: Variable null en la expresión
✅ Solución: Agregar verificación
   @PreAuthorize("#id != null and (#id == authentication.principal.id or hasRole('ADMIN'))")
```

## ✅ Verificar que Funciona

### Test 1: Usuario Normal
```bash
# Login como usuario normal
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@test.com","password":"1234"}' \
  | jq -r '.accessToken')

# Debe funcionar: su propio perfil
curl -X GET http://localhost:8082/api/users/5 \
  -H "Authorization: Bearer $TOKEN"

# Debe fallar: perfil de otro usuario
curl -X GET http://localhost:8082/api/users/1 \
  -H "Authorization: Bearer $TOKEN"
# Response: 403 Forbidden

# Debe fallar: listar todos (solo admin)
curl -X GET http://localhost:8082/api/users \
  -H "Authorization: Bearer $TOKEN"
# Response: 403 Forbidden
```

### Test 2: Usuario Admin
```bash
# Login como admin
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"Admin123!"}' \
  | jq -r '.accessToken')

# Debe funcionar: cualquier perfil
curl -X GET http://localhost:8082/api/users/5 \
  -H "Authorization: Bearer $TOKEN"

# Debe funcionar: listar todos
curl -X GET http://localhost:8082/api/users \
  -H "Authorization: Bearer $TOKEN"

# Debe funcionar: eliminar usuario
curl -X DELETE http://localhost:8082/api/users/5 \
  -H "Authorization: Bearer $TOKEN"
```

## 📚 Recursos

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html)
- [Method Security Expressions](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

---

**Nota**: Los roles en la base de datos deben estar en MAYÚSCULAS y sin el prefijo "ROLE_". Spring Security agregará el prefijo automáticamente.
