import { useState, useEffect } from 'react';
import { FaUser, FaEnvelope, FaPhone, FaGlobe, FaClock } from 'react-icons/fa';
import userService from '../../services/userService';
import ChangePassword from './ChangePassword';

const UserProfile = () => {
  const [user, setUser] = useState(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [activeTab, setActiveTab] = useState('profile'); // 'profile' o 'password'

  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    telefono: '',
    idioma: 'es',
    zonaHoraria: 'America/Bogota'
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const data = await userService.getCurrentUser();
      setUser(data);
      setFormData({
        nombre: data.nombre || '',
        apellido: data.apellido || '',
        telefono: data.telefono || '',
        idioma: data.idioma || 'es',
        zonaHoraria: data.zonaHoraria || 'America/Bogota'
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar perfil');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setSuccess(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setSaving(true);

    try {
      await userService.updateUser(user.id, formData);
      setSuccess(true);
      setEditing(false);
      loadProfile();
    } catch (err) {
      setError(err.response?.data?.message || 'Error al actualizar perfil');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-xl">Cargando perfil...</div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Mi Perfil</h1>

      {/* Tabs */}
      <div className="mb-6 border-b">
        <div className="flex gap-4">
          <button
            onClick={() => setActiveTab('profile')}
            className={`pb-3 px-4 font-semibold border-b-2 transition ${
              activeTab === 'profile'
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-600 hover:text-gray-800'
            }`}
          >
            Información Personal
          </button>
          <button
            onClick={() => setActiveTab('password')}
            className={`pb-3 px-4 font-semibold border-b-2 transition ${
              activeTab === 'password'
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-600 hover:text-gray-800'
            }`}
          >
            Cambiar Contraseña
          </button>
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'profile' ? (
        <div className="card">
          {success && (
            <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
              ✓ Perfil actualizado exitosamente
            </div>
          )}

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {!editing ? (
            // Vista de solo lectura
            <div className="space-y-6">
              <div className="flex items-center gap-4 pb-4 border-b">
                <div className="w-20 h-20 bg-primary-600 rounded-full flex items-center justify-center text-white text-3xl font-bold">
                  {user?.nombre?.charAt(0)}{user?.apellido?.charAt(0)}
                </div>
                <div>
                  <h2 className="text-2xl font-bold">
                    {user?.nombre} {user?.apellido}
                  </h2>
                  <p className="text-gray-600">{user?.email}</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                    <FaUser /> Nombre
                  </label>
                  <p className="text-gray-600">{user?.nombre || 'No especificado'}</p>
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                    <FaUser /> Apellido
                  </label>
                  <p className="text-gray-600">{user?.apellido || 'No especificado'}</p>
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                    <FaEnvelope /> Email
                  </label>
                  <p className="text-gray-600">{user?.email}</p>
                  <span className={`text-xs ${user?.emailVerificado ? 'text-green-600' : 'text-orange-600'}`}>
                    {user?.emailVerificado ? '✓ Verificado' : '⚠ Pendiente verificación'}
                  </span>
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                    <FaPhone /> Teléfono
                  </label>
                  <p className="text-gray-600">{user?.telefono || 'No especificado'}</p>
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                    <FaGlobe /> Idioma
                  </label>
                  <p className="text-gray-600">
                    {user?.idioma === 'es' ? 'Español' : 
                     user?.idioma === 'en' ? 'English' : user?.idioma || 'Español'}
                  </p>
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                    <FaClock /> Zona Horaria
                  </label>
                  <p className="text-gray-600">{user?.zonaHoraria || 'America/Bogota'}</p>
                </div>
              </div>

              <div className="pt-4 border-t">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-gray-700 font-semibold mb-2">Rol</label>
                    <span className="px-3 py-1 bg-blue-100 text-primary-800 rounded-full text-sm">
                      {user?.rol?.nombre || 'Usuario'}
                    </span>
                  </div>

                  <div>
                    <label className="block text-gray-700 font-semibold mb-2">Estado</label>
                    <span className={`px-3 py-1 rounded-full text-sm ${
                      user?.activo 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {user?.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="pt-4">
                <button
                  onClick={() => setEditing(true)}
                  className="btn-primary"
                >
                  Editar Perfil
                </button>
              </div>
            </div>
          ) : (
            // Formulario de edición
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-gray-700 font-semibold mb-2">
                    Nombre
                  </label>
                  <input
                    type="text"
                    name="nombre"
                    className="input-field"
                    value={formData.nombre}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2">
                    Apellido
                  </label>
                  <input
                    type="text"
                    name="apellido"
                    className="input-field"
                    value={formData.apellido}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2">
                    Teléfono
                  </label>
                  <input
                    type="tel"
                    name="telefono"
                    className="input-field"
                    value={formData.telefono}
                    onChange={handleChange}
                  />
                </div>

                <div>
                  <label className="block text-gray-700 font-semibold mb-2">
                    Idioma
                  </label>
                  <select
                    name="idioma"
                    className="input-field"
                    value={formData.idioma}
                    onChange={handleChange}
                  >
                    <option value="es">Español</option>
                    <option value="en">English</option>
                    <option value="fr">Français</option>
                    <option value="pt">Português</option>
                  </select>
                </div>

                <div className="md:col-span-2">
                  <label className="block text-gray-700 font-semibold mb-2">
                    Zona Horaria
                  </label>
                  <select
                    name="zonaHoraria"
                    className="input-field"
                    value={formData.zonaHoraria}
                    onChange={handleChange}
                  >
                    <option value="America/Bogota">Bogotá (GMT-5)</option>
                    <option value="America/Mexico_City">Ciudad de México (GMT-6)</option>
                    <option value="America/New_York">Nueva York (GMT-5)</option>
                    <option value="America/Los_Angeles">Los Ángeles (GMT-8)</option>
                    <option value="America/Argentina/Buenos_Aires">Buenos Aires (GMT-3)</option>
                    <option value="Europe/Madrid">Madrid (GMT+1)</option>
                  </select>
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={saving}
                >
                  {saving ? 'Guardando...' : 'Guardar Cambios'}
                </button>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setEditing(false);
                    setError('');
                    setSuccess(false);
                  }}
                >
                  Cancelar
                </button>
              </div>
            </form>
          )}
        </div>
      ) : (
        // Tab de cambiar contraseña
        <ChangePassword />
      )}
    </div>
  );
};

export default UserProfile;
