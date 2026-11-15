import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link, useNavigate } from 'react-router-dom';
import { FaPlus, FaEdit, FaTrash, FaUserShield, FaLock, FaKey, FaCopy } from 'react-icons/fa';
import roleService from '../../services/roleService';

const RoleList = () => {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const load = async () => {
    setLoading(true);
    try { setRoles(await roleService.getAllRoles()); }
    catch (e) { setError(e.response?.data?.message || 'Error al cargar roles'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const personalizar = async (r) => {
    try { const copia = await roleService.personalizar(r.id); navigate(`/roles/${copia.id}/permisos`); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo personalizar el rol'); }
  };

  const eliminar = async (r) => {
    if (r.esSistema) return;
    if (!window.confirm(`¿Eliminar el rol "${r.nombre}"?`)) return;
    try { await roleService.deleteRole(r.id); load(); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo eliminar'); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(roles, { clave: 'roles' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaUserShield className="text-primary-600" /> Roles</h1>
        <Link to="/roles/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nuevo Rol</Link>
      </div>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead><tr className="text-left text-gray-500 border-b">
            <th className="py-2 px-3">Nombre</th><th className="py-2 px-3">Descripción</th>
            <th className="py-2 px-3">Tipo</th><th className="py-2 px-3">Estado</th><th className="py-2 px-3 text-right">Acciones</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map((r) => (
              <tr key={r.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-3 font-medium">{r.nombre}</td>
                <td className="py-2 px-3 text-gray-600">{r.descripcion || '—'}</td>
                <td className="py-2 px-3">
                  {r.organizacionId == null
                    ? <span className="inline-flex items-center gap-1 text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded"><FaLock /> Del sistema</span>
                    : <span className="text-xs bg-primary-50 text-primary-700 px-2 py-0.5 rounded">De tu organización</span>}
                </td>
                <td className="py-2 px-3">
                  <span className={`text-xs px-2 py-0.5 rounded ${r.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {r.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="py-2 px-3 text-right">
                  <div className="flex justify-end gap-3">
                    <Link to={`/roles/${r.id}/permisos`} className="text-amber-600" title={r.editable ? 'Módulos y permisos' : 'Ver permisos'}><FaKey /></Link>
                    {r.editable ? (<>
                      <Link to={`/roles/${r.id}/edit`} className="text-primary-600" title="Editar"><FaEdit /></Link>
                      <button onClick={() => eliminar(r)} disabled={r.esSistema}
                        className={r.esSistema ? 'text-gray-300 cursor-not-allowed' : 'text-red-600'} title={r.esSistema ? 'Rol del sistema' : 'Eliminar'}>
                        <FaTrash />
                      </button>
                    </>) : (
                      <button onClick={() => personalizar(r)} className="text-primary-600 text-sm inline-flex items-center gap-1"
                        title="Crea una copia editable de este rol en tu organización"><FaCopy /> Personalizar</button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {roles.length === 0 && <div className="text-center py-8 text-gray-500">No hay roles.</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default RoleList;
