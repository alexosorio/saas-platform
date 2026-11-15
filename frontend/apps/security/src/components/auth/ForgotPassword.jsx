import { useState } from 'react';
import { Link } from 'react-router-dom';
import authService from '../../services/authService';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [method, setMethod] = useState('code'); // 'code' o 'link'
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (method === 'code') {
        await authService.forgotPassword(email);
      } else {
        await authService.requestPasswordResetLink(email);
      }
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al solicitar recuperación');
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
            ¡Correo Enviado!
          </h2>
          <p className="text-gray-600 mb-4">
            {method === 'code' 
              ? 'Revisa tu email. Te hemos enviado un código de 6 dígitos.'
              : 'Revisa tu email. Te hemos enviado un enlace de recuperación.'}
          </p>
          <Link
            to={method === 'code' ? '/reset-password' : '/login'}
            className="btn-primary inline-block"
          >
            {method === 'code' ? 'Ingresar Código' : 'Volver al Login'}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="card max-w-md w-full">
        <h2 className="text-3xl font-bold text-center mb-2">
          ¿Olvidaste tu Contraseña?
        </h2>
        <p className="text-center text-gray-600 mb-6">
          Te enviaremos instrucciones para recuperarla
        </p>

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
              placeholder="tu@email.com"
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 mb-2">Método de recuperación</label>
            <div className="space-y-2">
              <label className="flex items-center">
                <input
                  type="radio"
                  name="method"
                  value="code"
                  checked={method === 'code'}
                  onChange={(e) => setMethod(e.target.value)}
                  className="mr-2"
                />
                <span>Código de 6 dígitos (recomendado)</span>
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  name="method"
                  value="link"
                  checked={method === 'link'}
                  onChange={(e) => setMethod(e.target.value)}
                  className="mr-2"
                />
                <span>Enlace de recuperación</span>
              </label>
            </div>
          </div>

          <button
            type="submit"
            className="btn-primary w-full"
            disabled={loading}
          >
            {loading ? 'Enviando...' : 'Enviar Instrucciones'}
          </button>
        </form>

        <div className="mt-6 text-center space-y-2">
          <Link to="/login" className="text-primary-600 hover:underline block">
            Volver al login
          </Link>
          <Link to="/register" className="text-primary-600 hover:underline block">
            ¿No tienes cuenta? Regístrate
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
