import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppShell } from '@saas/ui-shell';
import { FaCrown, FaHome, FaListAlt, FaCreditCard, FaMoneyBillWave, FaShieldAlt, FaBoxOpen } from 'react-icons/fa';
import SubscriptionRoutes from './remote/Routes';

const P = '/subscription';
export const subscriptionShell = {
  brandName: 'Suscripción', brandIcon: <FaCrown />, panelTitle: 'Panel de Suscripción',
  userApiBaseUrl: import.meta.env.VITE_USER_API_URL || 'http://localhost:8082',
  home: { to: P, label: 'Inicio', icon: <FaHome /> },
  menu: [
    { label: 'Suscripción', items: [
      { to: `${P}/plans`, label: 'Planes', icon: <FaListAlt /> },
      { to: `${P}/subscription`, label: 'Mi Suscripción', icon: <FaCrown /> },
      { to: `${P}/payment-methods`, label: 'Métodos de Pago', icon: <FaCreditCard /> },
      { to: `${P}/payments`, label: 'Pagos', icon: <FaMoneyBillWave /> },
    ]},
  ],
  appLinks: [
    { href: '/', label: 'Seguridad', icon: <FaShieldAlt /> },
    { href: '/catalog', label: 'Catálogo', icon: <FaBoxOpen /> },
  ],
};

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/subscription/*" element={<AppShell {...subscriptionShell}><SubscriptionRoutes /></AppShell>} />
        <Route path="*" element={<Navigate to="/subscription" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
