import { lazy, Suspense, Component, useState, useEffect } from 'react';
import organizacionService from './services/organizacionService';
import userService from './services/userService';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { AppShell } from '@saas/ui-shell';
import {
  FaHome, FaThLarge, FaUsers, FaUserShield, FaUserCircle, FaShieldAlt, FaBoxOpen, FaCrown, FaCog, FaHistory,
  FaTags, FaSlidersH, FaUpload, FaBuilding, FaWarehouse, FaClipboardCheck, FaTruck,
  FaMoneyBillWave, FaShoppingCart, FaUndo, FaListAlt, FaCreditCard,
} from 'react-icons/fa';
import Login from './components/auth/Login';
import RegistroWizard from './components/auth/RegistroWizard';
import Configuracion from './components/configuracion/Configuracion';
import Auditoria from './components/auditoria/Auditoria';
import ForgotPassword from './components/auth/ForgotPassword';
import ResetPassword from './components/auth/ResetPassword';
import VerificarEmail from './components/auth/VerificarEmail';
import VerificacionGate from './components/VerificacionGate';
import UserList from './components/users/UserList';
import UserForm from './components/users/UserForm';
import UserProfile from './components/users/UserProfile';
import RoleList from './components/roles/RoleList';
import RoleForm from './components/roles/RoleForm';
import RolePermissions from './components/roles/RolePermissions';
import SubscriptionGate from './components/SubscriptionGate';
import SessionWatcher from './components/SessionWatcher';
import PoliticaDatos from './components/PoliticaDatos';

// Remotes (Module Federation)
const CatalogRoutes = lazy(() => import('catalog/Routes'));
const CatalogPublicList = lazy(() => import('catalog/PublicList'));
const CatalogDashboard = lazy(() => import('catalog/Dashboard'));
const SubscriptionRoutes = lazy(() => import('subscription/Routes'));

const Loading = () => (
  <div className="flex justify-center items-center py-20 text-gray-500">Cargando módulo…</div>
);

class RemoteBoundary extends Component {
  constructor(p) { super(p); this.state = { error: null }; }
  static getDerivedStateFromError(error) { return { error }; }
  componentDidCatch(error, info) { console.error('Remote load error:', error, info); }
  render() {
    if (this.state.error) {
      return (
        <div className="m-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-800 text-sm">
          <p className="font-semibold mb-1">No se pudo cargar el módulo remoto.</p>
          <pre className="whitespace-pre-wrap text-xs">{String(this.state.error?.message || this.state.error)}</pre>
        </div>
      );
    }
    return this.props.children;
  }
}

// Envuelve un remote con boundary + suspense
const Remote = ({ children }) => (
  <RemoteBoundary><Suspense fallback={<Loading />}>{children}</Suspense></RemoteBoundary>
);

const PrivateRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div className="flex justify-center items-center h-screen">Cargando...</div>;
  return isAuthenticated ? children : <Navigate to="/login" />;
};

const Dashboard = () => {
  const { user } = useAuth();
  const hora = new Date().getHours();
  const saludo = hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  return (
    <div>
      <div className="mb-5">
        <h1 className="text-2xl font-bold">{saludo}{user?.email ? `, ${user.email.split('@')[0]}` : ''}</h1>
        <p className="text-sm text-gray-500">{user?.rol ? `Rol: ${user.rol}` : 'Panel de la plataforma'}</p>
      </div>
      <Remote><CatalogDashboard /></Remote>
    </div>
  );
};


// Menú combinado de toda la plataforma (una sola SPA)
const C = '/catalog', S = '/subscription';
const shellConfig = {
  brandName: 'SaaS Platform', brandIcon: <FaShieldAlt />, panelTitle: 'Plataforma',
  userApiBaseUrl: import.meta.env.VITE_USER_API_URL || 'http://localhost:8082',
  home: { to: '/dashboard', label: 'Inicio', icon: <FaThLarge /> },
  menu: [
    { label: 'Administración', items: [
      { to: '/users', label: 'Usuarios', icon: <FaUsers /> },
      { to: '/roles', label: 'Roles', icon: <FaUserShield /> },
      { to: '/profile', label: 'Mi Perfil', icon: <FaUserCircle /> },
      { to: '/configuracion', label: 'Configuración', icon: <FaCog /> },
      { to: '/auditoria', label: 'Auditoría', icon: <FaHistory /> },
    ]},
    { label: 'Catálogo', items: [
      { to: `${C}`, label: 'Inicio Catálogo', icon: <FaBoxOpen /> },
      { to: `${C}/products`, label: 'Productos', icon: <FaThLarge /> },
      { to: `${C}/products/bulk`, label: 'Carga masiva (CSV)', icon: <FaUpload /> },
      { to: `${C}/categories`, label: 'Categorías', icon: <FaTags /> },
      { to: `${C}/brands`, label: 'Marcas', icon: <FaTags /> },
      { to: `${C}/characteristics`, label: 'Características', icon: <FaSlidersH /> },
    ]},
    { label: 'Inventario', items: [
      { to: `${C}/branches`, label: 'Sedes', icon: <FaBuilding /> },
      { to: `${C}/warehouses`, label: 'Bodegas', icon: <FaWarehouse /> },
      { to: `${C}/inventory`, label: 'Inventario', icon: <FaClipboardCheck /> },
    ]},
    { label: 'Ventas', items: [
      { to: `${C}/clients`, label: 'Clientes', icon: <FaUsers /> },
      { to: `${C}/suppliers`, label: 'Proveedores', icon: <FaTruck /> },
      { to: `${C}/lists`, label: 'Listas de Precios', icon: <FaMoneyBillWave /> },
      { to: `${C}/orders`, label: 'Órdenes', icon: <FaShoppingCart /> },
      { to: `${C}/devoluciones`, label: 'Devoluciones', icon: <FaUndo /> },
    ]},
    { label: 'Finanzas', items: [
      { to: `${C}/cuentas-por-pagar`, label: 'Cuentas por Pagar', icon: <FaMoneyBillWave /> },
      { to: `${C}/cuentas-por-cobrar`, label: 'Cuentas por Cobrar', icon: <FaMoneyBillWave /> },
    ]},
    { label: 'Suscripción', items: [
      { to: `${S}/plans`, label: 'Planes', icon: <FaListAlt /> },
      { to: `${S}/subscription`, label: 'Mi Suscripción', icon: <FaCrown /> },
      { to: `${S}/payment-methods`, label: 'Métodos de Pago', icon: <FaCreditCard /> },
      { to: `${S}/payments`, label: 'Pagos', icon: <FaMoneyBillWave /> },
    ]},
  ],
  appLinks: [],
};

// Permisos del token (claim 'permisos'). Solo para mostrar u ocultar menús: el backend es quien autoriza.
const permisosDelToken = () => {
  try {
    let b = (localStorage.getItem('accessToken') || '').split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b.length % 4) b += '=';
    return JSON.parse(atob(b)).permisos || [];
  } catch { return []; }
};

const Shell = ({ children }) => {
  const { user, logout } = useAuth();
  // Marca de la organización (nombre y logo) en el menú; se refresca al cambiar el logo en Configuración
  const [marca, setMarca] = useState(null);
  // Nombre y apellido del perfil para la barra superior (las sesiones anteriores solo guardaban el correo)
  const [perfil, setPerfil] = useState(null);
  useEffect(() => {
    userService.getCurrentUser().then((me) => {
      setPerfil(me);
      try {
        const guardado = JSON.parse(localStorage.getItem('user') || '{}');
        localStorage.setItem('user', JSON.stringify({ ...guardado, nombre: me.nombre, apellido: me.apellido }));
      } catch { /* sin almacenamiento: solo se muestra */ }
    }).catch(() => {});
  }, []);
  useEffect(() => {
    organizacionService.getMarca().then(setMarca).catch(() => {});
    const h = (e) => setMarca(e.detail);
    window.addEventListener('marca-actualizada', h);
    return () => window.removeEventListener('marca-actualizada', h);
  }, []);
  const puedePlataforma = permisosDelToken().includes('plataforma_suscripciones:leer');
  const menu = puedePlataforma
    ? [...shellConfig.menu, { label: 'Plataforma', items: [
        { to: '/subscription/admin', label: 'Suscripciones', icon: <FaCrown /> },
        { to: '/subscription/admin/pagos', label: 'Validación de pagos', icon: <FaCreditCard /> },
        { to: '/subscription/admin/facturacion', label: 'Facturación', icon: <FaMoneyBillWave /> },
      ]}]
    : shellConfig.menu;
  return <AppShell {...shellConfig} brandName={marca?.nombre || shellConfig.brandName} brandLogo={marca?.logo || null}
    menu={menu} user={perfil ? { ...user, nombre: perfil.nombre, apellido: perfil.apellido, email: perfil.email || user?.email } : user}
    onLogout={logout}>{children}</AppShell>;
};

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Públicas */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<RegistroWizard />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verificar-email" element={<VerificarEmail />} />
          <Route path="/legal/politica-datos" element={<PoliticaDatos />} />
          {/* Lista pública del catálogo (sin login ni shell) */}
          <Route path="/catalog/public/list/:token" element={<Remote><CatalogPublicList /></Remote>} />

          {/* Protegidas (shell + remotes) */}
          <Route path="/*" element={
            <PrivateRoute>
              <Shell>
                <SessionWatcher />
                <VerificacionGate>
                <SubscriptionGate>
                <Routes>
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/users" element={<UserList />} />
                  <Route path="/users/new" element={<UserForm />} />
                  <Route path="/users/:id/edit" element={<UserForm />} />
                  <Route path="/roles" element={<RoleList />} />
                  <Route path="/roles/new" element={<RoleForm />} />
                  <Route path="/roles/:id/edit" element={<RoleForm />} />
                  <Route path="/roles/:id/permisos" element={<RolePermissions />} />
                  <Route path="/profile" element={<UserProfile />} />
                  <Route path="/configuracion" element={<Configuracion />} />
                  <Route path="/auditoria" element={<Auditoria />} />
                  <Route path="/catalog/*" element={<Remote><CatalogRoutes /></Remote>} />
                  <Route path="/subscription/*" element={<Remote><SubscriptionRoutes /></Remote>} />
                  <Route path="/" element={<Navigate to="/dashboard" />} />
                </Routes>
                </SubscriptionGate>
                </VerificacionGate>
              </Shell>
            </PrivateRoute>
          } />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
