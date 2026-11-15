import { Link, useNavigate } from 'react-router-dom';
import { FaUser, FaSignOutAlt, FaHome } from 'react-icons/fa';
import { useAuth } from '../../contexts/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-primary-600 text-white shadow-lg">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link to="/dashboard" className="text-xl font-bold flex items-center gap-2">
            <FaHome /> SaaS Platform
          </Link>

          <div className="flex items-center gap-6">
            <Link to="/dashboard" className="hover:text-blue-200">
              Dashboard
            </Link>
            <Link to="/users" className="hover:text-blue-200">
              Usuarios
            </Link>
            <Link to="/roles" className="hover:text-blue-200">
              Roles
            </Link>
            <Link to="/profile" className="hover:text-blue-200 flex items-center gap-2">
              <FaUser /> {user?.email}
            </Link>
            <button
              onClick={handleLogout}
              className="hover:text-blue-200 flex items-center gap-2"
            >
              <FaSignOutAlt /> Salir
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
