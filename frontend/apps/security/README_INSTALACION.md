# 🚀 Frontend React - Instalación y Uso

## ✅ Archivos Creados

```
src/
├── services/
│   ├── api.js ✅
│   ├── authService.js ✅
│   ├── userService.js ✅
│   └── roleService.js ✅
├── contexts/
│   └── AuthContext.jsx ✅
├── components/
│   ├── auth/
│   │   ├── Login.jsx ✅
│   │   └── Register.jsx ✅
│   ├── users/
│   │   └── UserList.jsx ✅
│   └── layout/
│       └── Navbar.jsx ✅
├── App.jsx ✅
├── main.jsx ✅
└── index.css ✅
```

## 📦 Instalación

```bash
# 1. Navegar al directorio
cd saas-user-frontend

# 2. Instalar dependencias
npm install

# 3. Iniciar en desarrollo
npm run dev
```

## 🌐 Acceso

Abrir en navegador: **http://localhost:3000**

## 🔑 Credenciales de Prueba

```
Email: admin@demo.com
Password: Admin123!
```

## ✅ Funcionalidades Implementadas

- [x] Login con JWT
- [x] Registro de usuarios
- [x] Logout
- [x] Dashboard
- [x] Lista de usuarios (solo admin)
- [x] Activar/Desactivar usuarios
- [x] Eliminar usuarios
- [x] Ver perfil
- [x] Rutas protegidas
- [x] Interceptor automático de tokens

## 🔧 Backend Requerido

El backend debe estar corriendo en: **http://localhost:8082**

```bash
# En otra terminal
cd user-service-cqrs-final
docker-compose up -d
```

## 📝 Próximos Pasos (Opcional)

Si quieres más funcionalidades, puedes agregar:

- Formulario de creación/edición de usuarios
- Gestión completa de roles
- Cambio de contraseña
- Verificación de email
- Recuperación de contraseña

## 🐛 Solución de Problemas

### Error de CORS
Si ves errores de CORS, asegúrate de que el backend tenga configurado:
```java
setAllowedOriginPatterns("*")
setAllowCredentials(true)
```

### Error 401
Token expirado o inválido. Hacer logout y login de nuevo.

### No se ven los usuarios
Solo el usuario admin puede ver la lista de usuarios.
