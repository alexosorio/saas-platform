import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaToggleOn, FaToggleOff, FaCopyright } from 'react-icons/fa';
import brandService from '../../services/brandService';

const BrandList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { load(); }, []);
  const load = async () => { try { setLoading(true); setItems(await brandService.getAll()); } catch(e){console.error(e);} finally { setLoading(false);} };
  const del = async (id) => { if(!window.confirm('¿Eliminar marca?'))return; try{ await brandService.delete(id); load(); }catch(e){alert(e.response?.data?.message||'Error');} };
  const toggle = async (b) => { try{ b.activo?await brandService.deactivate(b.id):await brandService.activate(b.id); load(); }catch(e){alert(e.response?.data?.message||'Error');} };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'marcas' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaCopyright className="text-primary-600" /> Marcas</h1>
        <Link to="/catalog/brands/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva</Link>
      </div>
      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Logo</th><th className="px-4 py-3 text-left">Codigo</th>
            <th className="px-4 py-3 text-left">Nombre</th><th className="px-4 py-3 text-center">Estado</th>
            <th className="px-4 py-3 text-right">Acciones</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(b => (
              <tr key={b.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3">{b.logoUrl ? <img src={b.logoUrl} alt={b.nombre} className="h-8" /> : <span className="text-gray-300">-</span>}</td>
                <td className="px-4 py-3 font-mono text-sm">{b.codigo}</td>
                <td className="px-4 py-3 font-medium">{b.nombre}</td>
                <td className="px-4 py-3 text-center">
                  <button onClick={() => toggle(b)} className={`text-2xl ${b.activo ? 'text-green-600' : 'text-gray-400'}`}>{b.activo ? <FaToggleOn/> : <FaToggleOff/>}</button>
                </td>
                <td className="px-4 py-3"><div className="flex justify-end gap-3 text-lg">
                  <Link to={`/catalog/brands/${b.id}/edit`} className="text-primary-600"><FaEdit /></Link>
                  <button onClick={() => del(b.id)} className="text-red-600"><FaTrash /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-10 text-gray-500">No hay marcas</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default BrandList;
