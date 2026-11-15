import { useState } from 'react';
import { FaKey } from 'react-icons/fa';
import userService from '../../services/userService';

const ChangePassword = () => {
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
    setSuccess(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    // Validaciones
    if (formData.newPassword !== formData.confirmPassword) {
      setError('Las contraseñas nuevas no coinciden');
      return;
    }

    if (formData.newPassword.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres');
      return;
    }

    if (formData.currentPassword === formData.newPassword) {
      setError('La nueva contraseña debe ser diferente a la actual');
      return;
    }

    setLoading(true);

    try {
      await userService.changePassword(
        formData.currentPassword,
        formData.newPassword
      );
      setSuccess(true);
      setFormData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cambiar contraseña');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card max-w-2xl">
      <div className="flex items-center gap-3 mb-6">
        <FaKey className="text-2xl text-primary-600" />
        <h2 className="text-2xl font-bold">Cambiar Contraseña</h2>
      </div>

      {success && (
        <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
          ✓ Contraseña actualizada exitosamente
        </div>
      )}

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-gray-700 font-semibold mb-2">
            Contraseña Actual
          </label>
          <input
            type="password"
            name="currentPassword"
            className="input-field"
            value={formData.currentPassword}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label className="block text-gray-700 font-semibold mb-2">
            Nueva Contraseña
          </label>
          <input
            type="password"
            name="newPassword"
            className="input-field"
            value={formData.newPassword}
            onChange={handleChange}
            required
            minLength={6}
          />
          <p className="text-xs text-gray-500 mt-1">Mínimo 6 caracteres</p>
        </div>

        <div>
          <label className="block text-gray-700 font-semibold mb-2">
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

        <div className="bg-blue-50 border border-blue-200 rounded p-3 text-sm text-primary-800">
          <strong>Consejos de seguridad:</strong>
          <ul className="list-disc list-inside mt-2 space-y-1">
            <li>Usa al menos 8 caracteres</li>
            <li>Combina mayúsculas, minúsculas y números</li>
            <li>Incluye caracteres especiales (!@#$%)</li>
            <li>No uses información personal</li>
          </ul>
        </div>

        <div className="flex gap-3 pt-4">
          <button
            type="submit"
            className="btn-primary"
            disabled={loading}
          >
            {loading ? 'Actualizando...' : 'Actualizar Contraseña'}
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => {
              setFormData({
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
              });
              setError('');
              setSuccess(false);
            }}
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
};

export default ChangePassword;
