import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaPlus, FaEdit, FaToggleOn, FaToggleOff, FaBuilding, FaMapMarkerAlt } from 'react-icons/fa';
import sedeService from '../../services/sedeService';

const BranchList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { load(); }, []);
  const load = async () => {
    try { setLoading(true); setItems(await sedeService.getAll()); }
    catch (e) { console.error(e); } finally { setLoading(false); }
  };
  // Las sedes no se eliminan: se activan o desactivan
  const cambiarEstado = async (sd) => {
    const accion = sd.activo === false ? 'activar' : 'desactivar';
    if (!window.confirm(`¿${accion.charAt(0).toUpperCase() + accion.slice(1)} la sede "${sd.nombre}"?`)) return;
    try { sd.activo === false ? await sedeService.activate(sd.id) : await sedeService.deactivate(sd.id); load(); }
    catch (e) { alert(e.response?.data?.message || 'No se pudo cambiar el estado'); }
  };
  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'sedes' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaBuilding className="text-primary-600" /> Sedes</h1>
        <Link to="/catalog/branches/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva sede</Link>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {pg.visibles.map(s => (
          <div key={s.id} className={`card ${!s.activo ? 'opacity-50' : ''}`}>
            <div className="flex justify-between items-start">
              <div>
                <h3 className="text-lg font-bold">{s.nombre}</h3>
                <p className="text-sm text-gray-500 flex items-center gap-1 mt-1"><FaMapMarkerAlt /> {s.ciudadNombre}</p>
              </div>
              <div className="flex gap-2">
                <Link to={`/catalog/branches/${s.id}/edit`} className="text-primary-600"><FaEdit /></Link>
                <button onClick={() => cambiarEstado(s)} title={s.activo === false ? 'Activar' : 'Desactivar'}
                  className={s.activo === false ? 'text-gray-400' : 'text-green-600'}>{s.activo === false ? <FaToggleOff /> : <FaToggleOn />}</button>
              </div>
            </div>
            <div className="mt-3 text-sm text-gray-600 space-y-1">
              {s.direccion && <p>{s.direccion}</p>}
              {s.telefono && <p>Tel: {s.telefono}</p>}
              {s.email && <p>{s.email}</p>}
            </div>
            {!s.activo && <span className="inline-block mt-2 px-2 py-1 bg-red-100 text-red-800 rounded text-xs">Inactiva</span>}
          </div>
        ))}
      </div>
      {items.length === 0 && <div className="card text-center py-8 text-gray-500">No hay sedes todavía. Crea la primera para poder asignarle bodegas.</div>}
      {pg.total > 0 && <div className="card p-0 mt-4"><Paginacion {...pg.paginador} /></div>}
    </div>
  );
};
export default BranchList;
