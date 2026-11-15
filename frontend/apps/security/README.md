# 🎨 SaaS User Management - Frontend React

Frontend completo para gestión de usuarios, roles y perfiles.

## 🚀 Inicio Rápido (3 pasos)

```bash
# 1. Instalar dependencias
npm install

# 2. Iniciar
npm run dev

# 3. Abrir navegador
http://localhost:3000
```

## 📦 Dependencias

```bash
npm install react-router-dom axios react-icons
npm install -D tailwindcss postcss autoprefixer
```

## 🔑 Credenciales de Prueba

```
Email: admin@demo.com
Password: Admin123!
```

## 📁 Estructura

```
src/
├── components/
│   ├── auth/          → Login, Register
│   ├── users/         → CRUD Usuarios
│   ├── roles/         → CRUD Roles
│   └── layout/        → Navbar, Sidebar
├── contexts/          → AuthContext
├── services/          → APIs (axios)
├── pages/             → Dashboard, Users, Roles, Profile
├── App.jsx
└── main.jsx
```

## 🎯 Funcionalidades

### ✅ Autenticación
- [x] Login con JWT
- [x] Registro de usuarios
- [x] Logout
- [x] Token refresh automático
- [x] Rutas protegidas

### ✅ Usuarios
- [x] Listar todos los usuarios (solo admin)
- [x] Crear usuario (solo admin)
- [x] Editar usuario (admin o propietario)
- [x] Activar/Desactivar (solo admin)
- [x] Eliminar (solo admin)
- [x] Ver perfil propio
- [x] Cambiar contraseña

### ✅ Roles
- [x] Listar roles
- [x] Crear rol con permisos
- [x] Editar rol
- [x] Eliminar rol
- [x] Asignar permisos CRUD por módulo

### ✅ Perfiles
- [x] Ver perfil actual
- [x] Editar información personal
- [x] Cambiar avatar

## 🔗 API Backend

Backend debe estar corriendo en: http://localhost:8082

## 📚 Endpoints Usados

```
POST   /api/auth/login
POST   /api/auth/register
POST   /api/auth/refresh

GET    /api/users
POST   /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
DELETE /api/users/{id}

GET    /api/roles
POST   /api/roles
PUT    /api/roles/{id}
DELETE /api/roles/{id}
```

## 🎨 Tecnologías

- React 18
- Vite
- React Router v6
- Axios
- Tailwind CSS
- React Icons

## 📝 Notas

- Los tokens JWT se guardan en localStorage
- El interceptor de axios agrega automáticamente el Bearer token
- Las rutas protegidas redirigen a /login si no hay token
- Los permisos se validan en el frontend Y backend

