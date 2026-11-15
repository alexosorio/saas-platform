import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import authService from '../../services/authService';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  const tokenFromUrl = searchParams.get('token');
  const [method, setMethod] = useState(tokenFromUrl ? 'token' : 'code');
  
  const [formData, setFormData] = useState({
    email: '',
    code: '',
    token: tokenFromUrl || '',
    newPassword: '',
    confirmPassword: ''
  });
  
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validaciones
    if (formData.newPassword !== formData.confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    if (formData.newPassword.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres');
      return;
    }

    setLoading(true);

    try {
      if (method === 'code') {
        await authService.resetPasswordWithCode(
          formData.email,
          formData.code,
          formData.newPassword
        );
      } else {
        await authService.resetPasswordWithToken(
          formData.token,
          formData.newPassword
        );
      }
      setSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al resetear contraseña');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="card max-w-md w-full text-center">
          <div className="text-green-600 text-6xl mb-4">✓</div>
          <h2 className="text-2xl font-bold text-green-600 mb-4">
            ¡Contraseña Actualizada!
          </h2>
          <p className="text-gray-600 mb-4">
            Tu contraseña ha sido cambiada exitosamente.
          </p>
          <p className="text-gray-600">
            Redirigiendo al login...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 py-8">
      <div className="card max-w-md w-full">
        <h2 className="text-3xl font-bold text-center mb-2">
          Restablecer Contraseña
        </h2>
        <p className="text-center text-gray-600 mb-6">
          {method === 'code' 
            ? 'Ingresa el código que recibiste por email'
            : 'Establece tu nueva contraseña'}
        </p>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {method === 'code' && (
            <>
              <div>
                <label className="block text-gray-700 mb-2">Email</label>
                <input
                  type="email"
                  name="email"
                  className="input-field"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="tu@email.com"
                  required
                />
              </div>

              <div>
                <label className="block text-gray-700 mb-2">
                  Código de verificación (6 dígitos)
                </label>
                <input
                  type="text"
                  name="code"
                  className="input-field text-center text-2xl tracking-widest"
                  value={formData.code}
                  onChange={handleChange}
                  placeholder="000000"
                  maxLength={6}
                  required
                />
                <p className="text-xs text-gray-500 mt-1">
                  Revisa tu email para obtener el código
                </p>
              </div>
            </>
          )}

          {method === 'token' && (
            <div className="bg-blue-50 border border-blue-200 rounded p-3 text-sm text-primary-800">
              Token de recuperación recibido. Establece tu nueva contraseña.
            </div>
          )}

          <div>
            <label className="block text-gray-700 mb-2">Nueva Contraseña</label>
            <input
              type="password"
              name="newPassword"
              className="input-field"
              value={formData.newPassword}
              onChange={handleChange}
              required
              minLength={6}
            />
          </div>

          <div>
            <label className="block text-gray-700 mb-2">
              Confirmar Nueva Contraseña
            </label>
            <input
              type="password"
              name="confirmPassword"
              className="input-field"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              minLength={6}
            />
          </div>

          <button
            type="submit"
            className="btn-primary w-full"
            disabled={loading}
          >
            {loading ? 'Actualizando...' : 'Actualizar Contraseña'}
          </button>
        </form>

        {method === 'code' && (
          <div className="mt-4 text-center">
            <button
              onClick={() => setMethod('token')}
              className="text-primary-600 hover:underline text-sm"
            >
              ¿Tienes un enlace de recuperación?
            </button>
          </div>
        )}

        <div className="mt-6 text-center">
          <Link to="/login" className="text-primary-600 hover:underline">
            Volver al login
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ResetPassword;
