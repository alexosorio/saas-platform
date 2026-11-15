import { Link } from 'react-router-dom';
import { FaCrown, FaSignOutAlt, FaArrowLeft, FaHome } from 'react-icons/fa';

const Navbar = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const handleLogout = () => {
    localStorage.clear();
    window.location.href = '/login';
  };

  return (
    <nav className="bg-primary-700 text-white shadow-lg">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link to="/subscription/" className="text-xl font-bold flex items-center gap-2">
            <FaCrown /> Suscripcion
          </Link>
          <div className="flex items-center gap-6">
            <Link to="/subscription/" className="hover:text-primary-200 flex items-center gap-1"><FaHome /> Inicio</Link>
            <Link to="/subscription/plans" className="hover:text-primary-200">Planes</Link>
            <Link to="/subscription" className="hover:text-primary-200">Mi Suscripcion</Link>
            <Link to="/subscription/payment-methods" className="hover:text-primary-200">Metodos de Pago</Link>
            <Link to="/subscription/payments" className="hover:text-primary-200">Pagos</Link>

            {/* Enlaces a otras micro-apps: mismo origen, rutas relativas */}
            <a href="/" className="hover:text-primary-200 flex items-center gap-1"><FaArrowLeft /> Seguridad</a>
            <a href="/catalog" className="hover:text-primary-200">Catalogo</a>

            <span className="text-primary-200 text-sm">{user?.email}</span>
            <button onClick={handleLogout} className="hover:text-primary-200 flex items-center gap-2">
              <FaSignOutAlt /> Salir
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
