import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaCheck, FaPaperPlane, FaUserCheck } from 'react-icons/fa';
import userService from '../../services/userService';

const UserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState({ tipo: '', texto: '' });
  const [ocupado, setOcupado] = useState(null);   // id del usuario con una acción en curso

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await userService.getAllUsers();
      setUsers(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar usuarios');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Estás seguro de eliminar este usuario?')) return;

    try {
      await userService.deleteUser(id);
      loadUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Error al eliminar usuario');
    }
  };

  const handleToggleActive = async (user) => {
    try {
      if (user.activo) {
        await userService.deactivateUser(user.id);
      } else {
        await userService.activateUser(user.id);
      }
      loadUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Error al cambiar estado');
    }
  };

  const reenviarVerificacion = async (user) => {
    setOcupado(user.id); setAviso({ tipo: '', texto: '' });
    try {
      const r = await userService.reenviarVerificacion(user.id);
      setAviso({ tipo: 'ok', texto: r.message });
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.response?.data?.message || 'No se pudo reenviar el enlace' });
    } finally { setOcupado(null); }
  };

  const verificarManual = async (user) => {
    if (!window.confirm(`¿Marcar como verificado el correo ${user.email} sin que el usuario abra el enlace?\n\nHazlo solo si confirmaste por otro medio que el correo es suyo. Quedará registrado en auditoría.`)) return;
    setOcupado(user.id); setAviso({ tipo: '', texto: '' });
    try {
      const r = await userService.verificarManual(user.id);
      setAviso({ tipo: 'ok', texto: r.message });
      loadUsers();
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.response?.data?.message || 'No se pudo verificar' });
    } finally { setOcupado(null); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(users, { clave: 'usuarios' });
  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-xl">Cargando...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        {error}
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold">Usuarios</h2>
        <Link to="/users/new" className="btn-primary flex items-center gap-2">
          <FaPlus /> Nuevo Usuario
        </Link>
      </div>

      {aviso.texto && (
        <div className={`px-4 py-3 rounded mb-4 text-sm ${aviso.tipo === 'ok' ? 'bg-green-50 border border-green-200 text-green-800' : 'bg-red-100 text-red-700'}`}>
          {aviso.texto}
        </div>
      )}

      <div className="card overflow-x-auto">
        <table className="min-w-full">
          <thead>
            <tr className="border-b">
              <th className="px-4 py-3 text-left">ID</th>
              <th className="px-4 py-3 text-left">Nombre</th>
              <th className="px-4 py-3 text-left">Email</th>
              <th className="px-4 py-3 text-left">Rol</th>
              <th className="px-4 py-3 text-left">Estado</th>
              <th className="px-4 py-3 text-left">Verificado</th>
              <th className="px-4 py-3 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {pg.visibles.map((user) => (
              <tr key={user.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3">{user.id}</td>
                <td className="px-4 py-3">{user.nombre} {user.apellido}</td>
                <td className="px-4 py-3">{user.email}</td>
                <td className="px-4 py-3">
                  <span className="px-2 py-1 bg-blue-100 text-primary-800 rounded text-sm">
                    {user.rol?.nombre || 'Sin rol'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => handleToggleActive(user)}
                    className={`px-2 py-1 rounded text-sm ${
                      user.activo
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {user.activo ? 'Activo' : 'Inactivo'}
                  </button>
                </td>
                <td className="px-4 py-3">
                  {user.emailVerificado ? (
                    <span className="inline-flex items-center gap-1 text-green-700 text-sm"><FaCheck /> Sí</span>
                  ) : (
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded bg-amber-100 text-amber-800 text-xs">Pendiente</span>
                      <button onClick={() => reenviarVerificacion(user)} disabled={ocupado === user.id}
                        className="text-primary-600 hover:text-primary-800 disabled:opacity-50" title="Reenviar enlace de verificación">
                        <FaPaperPlane />
                      </button>
                      <button onClick={() => verificarManual(user)} disabled={ocupado === user.id}
                        className="text-green-600 hover:text-green-800 disabled:opacity-50" title="Marcar como verificado manualmente">
                        <FaUserCheck />
                      </button>
                    </div>
                  )}
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <Link
                      to={`/users/${user.id}/edit`}
                      className="text-primary-600 hover:text-primary-800"
                      title="Editar usuario"
                    >
                      <FaEdit />
                    </Link>
                    <button
                      onClick={() => handleDelete(user.id)}
                      className="text-red-600 hover:text-red-800"
                      title="Eliminar usuario"
                    >
                      <FaTrash />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {users.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No hay usuarios registrados
          </div>
        )}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};

export default UserList;
