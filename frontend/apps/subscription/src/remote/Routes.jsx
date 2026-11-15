import { Routes, Route, Navigate } from 'react-router-dom';
import '../index.css';
import SubHome from '../components/home/SubHome';
import PlanList from '../components/plans/PlanList';
import CurrentSubscription from '../components/subscription/CurrentSubscription';
import PaymentMethodList from '../components/payment-methods/PaymentMethodList';
import PaymentList from '../components/payments/PaymentList';
import AdminSuscripciones from '../components/admin/AdminSuscripciones';
import AdminPagos from '../components/admin/AdminPagos';
import AdminFacturacion from '../components/admin/AdminFacturacion';

export default function SubscriptionRoutes() {
  return (
    <Routes>
      <Route index element={<SubHome />} />
      <Route path="plans" element={<PlanList />} />
      <Route path="subscription" element={<CurrentSubscription />} />
      <Route path="payment-methods" element={<PaymentMethodList />} />
      <Route path="payments" element={<PaymentList />} />
      <Route path="admin" element={<AdminSuscripciones />} />
      <Route path="admin/pagos" element={<AdminPagos />} />
      <Route path="admin/facturacion" element={<AdminFacturacion />} />
      <Route path="*" element={<Navigate to="/subscription" replace />} />
    </Routes>
  );
}
