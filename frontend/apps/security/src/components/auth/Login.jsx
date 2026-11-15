import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [avisoSesion] = useState(() => {
    const q = new URLSearchParams(window.location.search);
    const motivo = q.get('session') || (q.get('expired') === '1' ? 'expired' : null);
    if (motivo === 'logout') return 'Cerraste sesión en otra pestaña.';
    if (motivo === 'logout_ok') return 'Cerraste sesión correctamente.';
    if (motivo === 'inactividad') return 'Tu sesión se cerró por inactividad. Vuelve a iniciar sesión para continuar.';
    if (motivo) return 'Tu sesión finalizó. Vuelve a iniciar sesión para continuar.';
    return null;
  });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Error al iniciar sesión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="card max-w-md w-full">
        <h2 className="text-3xl font-bold text-center mb-6">Iniciar Sesión</h2>
        
        {avisoSesion && !error && (
          <div className="bg-amber-100 border border-amber-400 text-amber-800 px-4 py-3 rounded mb-4" role="alert">
            {avisoSesion}
          </div>
        )}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-700 mb-2">Email</label>
            <input
              type="email"
              className="input-field"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@demo.com"
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 mb-2">Contraseña</label>
            <input
              type="password"
              className="input-field"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Admin123!"
              required
            />
          </div>

          <div className="flex justify-end">
            <Link 
              to="/forgot-password" 
              className="text-sm text-primary-600 hover:underline"
            >
              ¿Olvidaste tu contraseña?
            </Link>
          </div>

          <button
            type="submit"
            className="btn-primary w-full"
            disabled={loading}
          >
            {loading ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        <p className="text-center mt-4 text-sm text-gray-600">
          Credenciales de prueba: admin@demo.com / Admin123!
        </p>

        <p className="text-center mt-2">
          ¿No tienes cuenta?{' '}
          <Link to="/register" className="text-primary-600 hover:underline">
            Regístrate
          </Link>
        </p>
        <p className="text-center mt-2 text-sm">
          <Link to="/verificar-email" className="text-gray-500 hover:text-primary-600 hover:underline">
            ¿No te llegó el correo de verificación?
          </Link>
        </p>
      </div>
    </div>
  );
};

export default Login;
