import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaPlus, FaEdit, FaTrash, FaTruck } from 'react-icons/fa';
import supplierService from '../../services/supplierService';

const SupplierList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { load(); }, []);
  const load = async () => {
    try { setLoading(true); setItems(await supplierService.getAll()); }
    catch (e) { console.error(e); } finally { setLoading(false); }
  };
  const del = async (id) => {
    if (!window.confirm('¿Desactivar este proveedor?')) return;
    await supplierService.delete(id); load();
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'proveedores' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaTruck className="text-primary-600" /> Proveedores</h1>
        <Link to="/catalog/suppliers/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nuevo proveedor</Link>
      </div>
      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Nombre</th>
            <th className="px-4 py-3 text-left">Identificación</th>
            <th className="px-4 py-3 text-left">Contacto</th>
            <th className="px-4 py-3 text-left">Teléfono</th>
            <th className="px-4 py-3 text-left">Email</th>
            <th className="px-4 py-3 text-center">Estado</th>
            <th></th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(s => (
              <tr key={s.id} className={`border-b ${!s.activo ? 'opacity-50' : ''}`}>
                <td className="px-4 py-3 font-semibold">{s.nombre}</td>
                <td className="px-4 py-3">{s.identificacion || '-'}</td>
                <td className="px-4 py-3">{s.contacto || '-'}</td>
                <td className="px-4 py-3">{s.telefono || '-'}</td>
                <td className="px-4 py-3">{s.email || '-'}</td>
                <td className="px-4 py-3 text-center">
                  {s.activo ? <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">Activo</span>
                            : <span className="px-2 py-1 bg-red-100 text-red-800 rounded text-xs">Inactivo</span>}
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  <Link to={`/catalog/suppliers/${s.id}/edit`} className="text-primary-600 mr-3 inline-block"><FaEdit /></Link>
                  <button onClick={() => del(s.id)} className="text-red-600"><FaTrash /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-8 text-gray-500">No hay proveedores</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default SupplierList;
