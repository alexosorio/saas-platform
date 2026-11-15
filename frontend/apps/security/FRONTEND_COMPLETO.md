# 🎨 Frontend React - SaaS User Management

## 📁 Estructura del Proyecto

```
saas-user-frontend/
├── public/
├── src/
│   ├── components/
│   │   ├── auth/
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   ├── users/
│   │   │   ├── UserList.jsx
│   │   │   ├── UserForm.jsx
│   │   │   ├── UserProfile.jsx
│   │   ├── roles/
│   │   │   ├── RoleList.jsx
│   │   │   ├── RoleForm.jsx
│   │   ├── layout/
│   │   │   ├── Navbar.jsx
│   │   │   ├── Sidebar.jsx
│   │   │   ├── PrivateRoute.jsx
│   ├── contexts/
│   │   └── AuthContext.jsx
│   ├── services/
│   │   ├── api.js
│   │   ├── authService.js
│   │   ├── userService.js
│   │   └── roleService.js
│   ├── pages/
│   │   ├── Dashboard.jsx
│   │   ├── Users.jsx
│   │   ├── Roles.jsx
│   │   └── Profile.jsx
│   ├── App.jsx
│   ├── main.jsx
│   └── index.css
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## 🚀 Inicio Rápido

```bash
# 1. Crear proyecto
npm create vite@latest saas-user-frontend -- --template react
cd saas-user-frontend

# 2. Instalar dependencias
npm install react-router-dom axios react-icons
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# 3. Copiar todos los archivos de esta guía

# 4. Iniciar
npm run dev
```

Abre http://localhost:3000

## 📝 Credenciales de Prueba

```
Email: admin@demo.com
Password: Admin123!
```

---

## 📄 ARCHIVOS DEL PROYECTO

A continuación, todos los archivos listos para copiar y pegar:

