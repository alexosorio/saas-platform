// Entrada STANDALONE del remote (para desarrollarlo solo: npm run dev).
// En producción, el host lo carga vía Module Federation (expone ./Routes y ./PublicList).
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppShell } from '@saas/ui-shell';
import { FaBoxOpen, FaHome, FaThLarge, FaTags, FaSlidersH, FaUpload, FaBuilding, FaWarehouse, FaClipboardCheck, FaUsers, FaTruck, FaMoneyBillWave, FaShoppingCart, FaUndo, FaShieldAlt, FaCreditCard } from 'react-icons/fa';
import CatalogRoutes from './remote/Routes';
import PublicListView from './components/lists/PublicListView';

const P = '/catalog';
export const catalogShell = {
  brandName: 'Catálogo', brandIcon: <FaBoxOpen />, panelTitle: 'Panel de Catálogo',
  userApiBaseUrl: import.meta.env.VITE_USER_API_URL || 'http://localhost:8082',
  home: { to: P, label: 'Inicio', icon: <FaHome /> },
  menu: [
    { label: 'Catálogo', items: [
      { to: `${P}/products`, label: 'Productos', icon: <FaThLarge /> },
      { to: `${P}/products/bulk`, label: 'Carga masiva (CSV)', icon: <FaUpload /> },
      { to: `${P}/categories`, label: 'Categorías', icon: <FaTags /> },
      { to: `${P}/brands`, label: 'Marcas', icon: <FaTags /> },
      { to: `${P}/characteristics`, label: 'Características', icon: <FaSlidersH /> },
    ]},
    { label: 'Inventario', items: [
      { to: `${P}/branches`, label: 'Sedes', icon: <FaBuilding /> },
      { to: `${P}/warehouses`, label: 'Bodegas', icon: <FaWarehouse /> },
      { to: `${P}/inventory`, label: 'Inventario', icon: <FaClipboardCheck /> },
    ]},
    { label: 'Finanzas', items: [
      { to: `${P}/cuentas-por-pagar`, label: 'Cuentas por Pagar', icon: <FaMoneyBillWave /> },
      { to: `${P}/cuentas-por-cobrar`, label: 'Cuentas por Cobrar', icon: <FaMoneyBillWave /> },
    ]},
    { label: 'Ventas', items: [
      { to: `${P}/clients`, label: 'Clientes', icon: <FaUsers /> },
      { to: `${P}/suppliers`, label: 'Proveedores', icon: <FaTruck /> },
      { to: `${P}/lists`, label: 'Listas de Precios', icon: <FaMoneyBillWave /> },
      { to: `${P}/orders`, label: 'Órdenes', icon: <FaShoppingCart /> },
      { to: `${P}/devoluciones`, label: 'Devoluciones', icon: <FaUndo /> },
    ]},
  ],
  appLinks: [
    { href: '/', label: 'Seguridad', icon: <FaShieldAlt /> },
    { href: '/subscription', label: 'Suscripción', icon: <FaCreditCard /> },
  ],
};

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/catalog/public/list/:token" element={<PublicListView />} />
        <Route path="/catalog/*" element={<AppShell {...catalogShell}><CatalogRoutes /></AppShell>} />
        <Route path="*" element={<Navigate to="/catalog" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
