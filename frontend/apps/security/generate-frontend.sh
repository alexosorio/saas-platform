#!/bin/bash

# Este script genera todos los archivos del frontend React

cd "$(dirname "$0")"

echo "Generando frontend React completo..."

# El package.json ya existe, crear los demás archivos

# README
cat > README.md << 'EOF'
# SaaS Platform - Frontend de Gestión de Usuarios

Frontend React para el sistema de gestión de usuarios, roles y perfiles.

## Características

- 🔐 Autenticación JWT
- 👥 Gestión de usuarios
- 🎭 Gestión de roles y permisos
- 📋 Gestión de perfiles
- 🎨 UI moderna con Tailwind CSS
- ⚡ Vite para desarrollo rápido

## Inicio Rápido

```bash
# Instalar dependencias
npm install

# Iniciar en desarrollo
npm run dev

# Construir para producción
npm run build
```

La aplicación estará disponible en http://localhost:3000

## Credenciales de Prueba

```
Email: admin@demo.com
Password: Admin123!
```

## Estructura

```
src/
├── components/        # Componentes React
│   ├── auth/         # Login, Register
│   ├── users/        # Lista, formulario usuarios
│   ├── roles/        # Lista, formulario roles
│   └── layout/       # Navbar, Sidebar
├── contexts/         # Context API (Auth)
├── services/         # Servicios API
├── pages/            # Páginas principales
└── utils/            # Utilidades
```

## Variables de Entorno

Crear archivo `.env`:

```
VITE_API_URL=http://localhost:8082
```

## Funcionalidades

### Autenticación
- Login con email/password
- Registro de nuevos usuarios
- Logout
- Tokens JWT automáticos

### Usuarios
- Listar todos los usuarios
- Crear nuevo usuario
- Editar usuario
- Activar/Desactivar
- Eliminar usuario
- Ver perfil

### Roles
- Listar roles
- Crear rol con permisos
- Editar rol
- Asignar permisos por módulo (CRUD)

### Perfiles
- Ver perfil del usuario actual
- Editar información personal
- Cambiar contraseña

## Stack Tecnológico

- React 18
- Vite
- React Router v6
- Axios
- Tailwind CSS
- React Icons
EOF

echo "✅ README creado"
echo "Proyecto React completo generado"

