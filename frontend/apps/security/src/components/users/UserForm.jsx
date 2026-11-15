import { useState, useEffect, useMemo } from 'react';
import { Autocomplete, lazyOptions } from '@saas/ui-shell';
import { useNavigate, useParams } from 'react-router-dom';
import { FaUser, FaEnvelope, FaLock, FaPhone, FaSave, FaTimes } from 'react-icons/fa';
import userService from '../../services/userService';
import roleService from '../../services/roleService';

const UserForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(isEdit);
  const [error, setError] = useState('');
  // Roles bajo demanda (se cargan al abrir el campo). Sin rol preseleccionado: debe elegirse explícitamente.
  const rolSrc = useMemo(() => lazyOptions(() => roleService.getAllRoles(), {
    sublabel: (r) => r.descripcion || '', filter: (r) => r.activo !== false,
  }), []);
  const [rolLabel, setRolLabel] = useState('');
  // Bodegas desde las que puede vender (catálogo). Sin bodegas = sin restricción.
  const CATALOG_API = import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8083';
  const authHeaders = () => ({ 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}` });
  const [bodegas, setBodegas] = useState([]);
  const [bodegasAsignadas, setBodegasAsignadas] = useState([]);
  useEffect(() => {
    fetch(`${CATALOG_API}/api/warehouses`, { headers: authHeaders() }).then(r => r.ok ? r.json() : []).then(l => setBodegas(l.filter(b => b.activo !== false))).catch(() => {});
    if (isEdit) fetch(`${CATALOG_API}/api/usuarios-bodegas/${id}`, { headers: authHeaders() }).then(r => r.ok ? r.json() : []).then(setBodegasAsignadas).catch(() => {});
    // eslint-disable-next-line
  }, [id]);
  const guardarBodegas = async (usuarioId) => {
    if (!usuarioId) return;
    const r = await fetch(`${CATALOG_API}/api/usuarios-bodegas/${usuarioId}`, { method: 'PUT', headers: authHeaders(), body: JSON.stringify({ bodegaIds: bodegasAsignadas }) });
    if (!r.ok) throw new Error('El usuario se guardó, pero no se pudieron asignar las bodegas');
  };

  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    nombre: '',
    apellido: '',
    telefono: '',
    rolId: '',
    activo: true
  });

  useEffect(() => {
    if (isEdit) {
      loadUser();
    }
  }, [id]);

  const loadUser = async () => {
    try {
      setLoadingData(true);
      const user = await userService.getUserById(id);
      setFormData({
        email: user.email || '',
        password: '',
        confirmPassword: '',
        nombre: user.nombre || '',
        apellido: user.apellido || '',
        telefono: user.telefono || '',
        rolId: user.rol?.id || '',
        activo: user.activo
      });
      setRolLabel(user.rol?.nombre || '');
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar usuario');
    } finally {
      setLoadingData(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validaciones
    if (!isEdit) {
      if (!formData.password) {
        setError('La contraseña es obligatoria');
        return;
      }
      if (formData.password !== formData.confirmPassword) {
        setError('Las contraseñas no coinciden');
        return;
      }
      if (formData.password.length < 6) {
        setError('La contraseña debe tener al menos 6 caracteres');
        return;
      }
    } else {
      // En edición, si se ingresó contraseña, validar
      if (formData.password && formData.password !== formData.confirmPassword) {
        setError('Las contraseñas no coinciden');
        return;
      }
      if (formData.password && formData.password.length < 6) {
        setError('La contraseña debe tener al menos 6 caracteres');
        return;
      }
    }

    setLoading(true);

    try {
      const payload = {
        email: formData.email,
        nombre: formData.nombre,
        apellido: formData.apellido,
        telefono: formData.telefono,
        rolId: parseInt(formData.rolId),
        activo: formData.activo
      };

      // Solo incluir password si se proporcionó
      if (formData.password) {
        payload.password = formData.password;
      }

      if (isEdit) {
        await userService.updateUser(id, payload);
        await guardarBodegas(id);
      } else {
        const creado = await userService.createUser(payload);
        await guardarBodegas(creado?.id);
      }

      navigate('/users');
    } catch (err) {
      setError(err.response?.data?.message || `Error al ${isEdit ? 'actualizar' : 'crear'} usuario`);
    } finally {
      setLoading(false);
    }
  };

  if (loadingData) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-xl">Cargando usuario...</div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">
          {isEdit ? 'Editar Usuario' : 'Nuevo Usuario'}
        </h1>
        <p className="text-gray-600 mt-2">
          {isEdit 
            ? 'Actualiza la información del usuario' 
            : 'Completa el formulario para crear un nuevo usuario'}
        </p>
      </div>

      <div className="card">
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Información Personal */}
          <div>
            <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <FaUser /> Información Personal
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 font-semibold mb-2">
                  Nombre *
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
                  Apellido *
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
                <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2">
                  <FaPhone /> Teléfono
                </label>
                <input
                  type="tel"
                  name="telefono"
                  className="input-field"
                  value={formData.telefono}
                  onChange={handleChange}
                  placeholder="+57 300 123 4567"
                />
              </div>

              <div>
                <label className="block text-gray-700 font-semibold mb-2">
                  Rol *
                </label>
                <Autocomplete source={rolSrc} value={formData.rolId} valueLabel={rolLabel}
                  onChange={(v, o) => { setFormData((f) => ({ ...f, rolId: v })); setRolLabel(o?.label || ''); }}
                  placeholder="Buscar rol…" required />
              </div>
            </div>
          </div>

          {/* Cuenta */}
          <div>
            <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <FaEnvelope /> Cuenta
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="md:col-span-2">
                <label className="block text-gray-700 font-semibold mb-2">
                  Email *
                </label>
                <input
                  type="email"
                  name="email"
                  className="input-field"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  disabled={isEdit}
                  placeholder="usuario@ejemplo.com"
                />
                {isEdit && (
                  <p className="text-xs text-gray-500 mt-1">
                    El email no se puede modificar
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Contraseña */}
          <div>
            <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <FaLock /> Contraseña {isEdit && '(dejar en blanco para no cambiar)'}
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 font-semibold mb-2">
                  Contraseña {!isEdit && '*'}
                </label>
                <input
                  type="password"
                  name="password"
                  className="input-field"
                  value={formData.password}
                  onChange={handleChange}
                  required={!isEdit}
                  minLength={6}
                  placeholder={isEdit ? 'Dejar vacío para no cambiar' : '••••••••'}
                />
              </div>

              <div>
                <label className="block text-gray-700 font-semibold mb-2">
                  Confirmar Contraseña {!isEdit && '*'}
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  className="input-field"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  required={!isEdit && !!formData.password}
                  minLength={6}
                  placeholder={isEdit ? 'Dejar vacío para no cambiar' : '••••••••'}
                />
              </div>

              {!isEdit && (
                <div className="md:col-span-2 bg-blue-50 border border-blue-200 rounded p-3 text-sm">
                  <strong>Requisitos de contraseña:</strong>
                  <ul className="list-disc list-inside mt-2 space-y-1 text-primary-800">
                    <li>Mínimo 6 caracteres</li>
                    <li>Se recomienda incluir mayúsculas, minúsculas y números</li>
                  </ul>
                </div>
              )}
            </div>
          </div>

          {/* Estado */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Estado</h3>
            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                name="activo"
                id="activo"
                className="w-5 h-5"
                checked={formData.activo}
                onChange={handleChange}
              />
              <label htmlFor="activo" className="text-gray-700 font-semibold cursor-pointer">
                Usuario Activo
              </label>
              <span className="text-sm text-gray-500">
                (Los usuarios inactivos no pueden iniciar sesión)
              </span>
            </div>
          </div>

          {/* Botones */}
            <div className="border-t pt-4">
              <label className="block font-semibold mb-1">Bodegas desde las que vende</label>
              <p className="text-xs text-gray-500 mb-2">Sin bodegas marcadas no hay restricción. Si marcas alguna, el usuario solo podrá incluir en órdenes
                unidades de esas bodegas, salvo que su rol tenga el permiso "Vender desde otras bodegas".</p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-1 max-h-48 overflow-y-auto">
                {bodegas.map(b => (
                  <label key={b.id} className="flex items-center gap-2 text-sm cursor-pointer">
                    <input type="checkbox" checked={bodegasAsignadas.includes(b.id)}
                      onChange={e => setBodegasAsignadas(a => e.target.checked ? [...a, b.id] : a.filter(x => x !== b.id))} />
                    {b.nombre} <span className="text-xs text-gray-400">{b.sedeNombre}</span>
                  </label>
                ))}
                {bodegas.length === 0 && <span className="text-sm text-gray-400">No hay bodegas activas.</span>}
              </div>
            </div>

          <div className="flex gap-3 pt-4 border-t">
            <button
              type="submit"
              className="btn-primary flex items-center gap-2"
              disabled={loading}
            >
              <FaSave />
              {loading 
                ? (isEdit ? 'Actualizando...' : 'Creando...') 
                : (isEdit ? 'Actualizar Usuario' : 'Crear Usuario')}
            </button>

            <button
              type="button"
              className="btn-secondary flex items-center gap-2"
              onClick={() => navigate('/users')}
            >
              <FaTimes />
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UserForm;
