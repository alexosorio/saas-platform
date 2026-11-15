# SaaS Frontend Workspace

Monorepo (npm workspaces) con un paquete de UI compartido y las tres micro-apps.

```
packages/ui-shell      → @saas/ui-shell  (AppShell, Topbar, Sidebar, ChangePasswordModal)
apps/catalog           → saas-catalog-frontend      (base /catalog)
apps/subscription      → saas-subscription-frontend (base /subscription)
apps/security          → saas-user-frontend         (base /)
```

## Instalar
```bash
npm install            # en la raíz: instala todo y enlaza @saas/ui-shell
npm run build:shell    # compila el paquete (esbuild -> dist/index.js)
```
El paquete se compila a `packages/ui-shell/dist/index.js`. **Cada vez que cambies el
shell, corre `npm run build:shell`** (o `npm run build -w @saas/ui-shell`).

## Desarrollo / build por app
```bash
npm run dev   -w saas-catalog-frontend      # o build:catalog / build:subscription / build:security
npm run build:catalog
npm run build:subscription
npm run build:security
```

## ¿Cómo consume cada app el shell?
1. `package.json` → dependencia `"@saas/ui-shell": "*"`.
2. `tailwind.config.js` → en `content` se incluye `../../packages/ui-shell/src/**/*.{js,jsx}`
   para que Tailwind genere las clases (usa el `primary` de cada app).
3. `src/main.jsx` → `import '@saas/ui-shell/shell.css';` (Roboto + clases Material).
4. `src/App.jsx` → envuelve las rutas con `<AppShell {...config}>`, pasando su propio menú.

## API de AppShell
```jsx
<AppShell
  brandName="Catálogo" brandIcon={<FaBoxOpen/>}
  panelTitle="Panel de Catálogo"
  userApiBaseUrl={import.meta.env.VITE_USER_API_URL || 'http://localhost:8082'}
  home={{ to:'/', label:'Inicio', icon:<FaHome/> }}
  menu={[{ label:'Sección', items:[{ to:'/x', label:'X', icon:<FaIcon/> }] }]}
  appLinks={[{ href:'/', label:'Seguridad', icon:<FaShieldAlt/> }]}
  user={user}            // opcional; por defecto lee localStorage 'user'
  onLogout={logout}      // opcional; por defecto limpia tokens y va a /login
>
  <Routes>...</Routes>
</AppShell>
```
El "Cambiar contraseña" llama `POST {userApiBaseUrl}/api/users/change-password`.

## Variables de entorno (cada app)
- `VITE_USER_API_URL=http://localhost:8082`  (user-service, para perfil/contraseña)
