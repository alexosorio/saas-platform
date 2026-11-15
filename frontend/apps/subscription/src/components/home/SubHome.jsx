import { Link } from 'react-router-dom';
import { FaListAlt, FaCrown, FaCreditCard, FaMoneyBillWave } from 'react-icons/fa';

const SubHome = () => {
  const cards = [
    { to: '/subscription/plans', icon: <FaListAlt />, title: 'Planes disponibles', desc: 'Comparar y elegir un plan' },
    { to: '/subscription/subscription', icon: <FaCrown />, title: 'Mi Suscripción', desc: 'Estado, plan actual e historial' },
    { to: '/subscription/payment-methods', icon: <FaCreditCard />, title: 'Métodos de Pago', desc: 'Tarjetas y formas de pago' },
    { to: '/subscription/payments', icon: <FaMoneyBillWave />, title: 'Pagos', desc: 'Historial y registro de pagos' },
  ];
  return (
    <div className="max-w-5xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Suscripción</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {cards.map(c => (
          <Link key={c.to} to={c.to} className="card hover:shadow-lg transition flex items-start gap-4">
            <div className="text-primary-700 text-3xl mt-1">{c.icon}</div>
            <div>
              <h3 className="text-lg font-bold text-primary-700">{c.title}</h3>
              <p className="text-gray-600 text-sm mt-1">{c.desc}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
};
export default SubHome;
