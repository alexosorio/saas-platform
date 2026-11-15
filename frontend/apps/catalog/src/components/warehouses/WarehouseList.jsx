import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaToggleOn, FaToggleOff, FaPlus, FaWarehouse } from 'react-icons/fa';
import warehouseService from '../../services/warehouseService';

const WarehouseList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { load(); }, []);
  const load = async () => { try { setLoading(true); setItems(await warehouseService.getAll()); } catch(e){console.error(e);} finally { setLoading(false);} };
  // Las bodegas no se eliminan: se activan o desactivan
  const cambiarEstado = async (b) => {
    const accion = b.activo === false ? 'activar' : 'desactivar';
    if (!window.confirm(`¿${accion.charAt(0).toUpperCase() + accion.slice(1)} la bodega "${b.nombre}"?`)) return;
    try { b.activo === false ? await warehouseService.activate(b.id) : await warehouseService.deactivate(b.id); load(); }
    catch (e) { alert(e.response?.data?.message || 'No se pudo cambiar el estado'); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'bodegas' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaWarehouse className="text-primary-600" /> Bodegas</h1>
        <Link to="/catalog/warehouses/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva</Link>
      </div>
      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Codigo</th><th className="px-4 py-3 text-left">Nombre</th>
            <th className="px-4 py-3 text-left">Sede</th><th className="px-4 py-3 text-right">Capacidad m2</th>
            <th className="px-4 py-3 text-right">Acciones</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(b => (
              <tr key={b.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-sm">{b.codigo}</td>
                <td className="px-4 py-3 font-medium">{b.nombre}</td>
                <td className="px-4 py-3">{b.sedeNombre || ("Sede #" + b.sedeId)}</td>
                <td className="px-4 py-3 text-right">{b.capacidadM2 || '-'}</td>
                <td className="px-4 py-3"><div className="flex justify-end gap-3 text-lg">
                  <Link to={`/catalog/warehouses/${b.id}/edit`} className="text-primary-600"><FaEdit /></Link>
                  <button onClick={() => cambiarEstado(b)} title={b.activo === false ? 'Activar' : 'Desactivar'}
                    className={b.activo === false ? 'text-gray-400' : 'text-green-600'}>{b.activo === false ? <FaToggleOff /> : <FaToggleOn />}</button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-10 text-gray-500">No hay bodegas</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default WarehouseList;
