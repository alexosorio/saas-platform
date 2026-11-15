import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaToggleOn, FaToggleOff, FaTags } from 'react-icons/fa';
import categoryService from '../../services/categoryService';

const CategoryList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { load(); }, []);
  const load = async () => {
    try { setLoading(true); setItems(await categoryService.getAll()); }
    catch (e) { console.error(e); } finally { setLoading(false); }
  };
  const del = async (id) => {
    if (!window.confirm('¿Eliminar categoria?')) return;
    try { await categoryService.delete(id); load(); } catch (e) { alert(e.response?.data?.message || 'Error'); }
  };
  const toggle = async (c) => {
    try { c.activo ? await categoryService.deactivate(c.id) : await categoryService.activate(c.id); load(); }
    catch (e) { alert(e.response?.data?.message || 'Error'); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'categorias' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaTags className="text-primary-600" /> Categorias</h1>
        <Link to="/catalog/categories/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva</Link>
      </div>
      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Codigo</th><th className="px-4 py-3 text-left">Nombre</th>
            <th className="px-4 py-3 text-left">Descripcion</th><th className="px-4 py-3 text-center">Estado</th>
            <th className="px-4 py-3 text-right">Acciones</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(c => (
              <tr key={c.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-sm">{c.codigo}</td>
                <td className="px-4 py-3 font-medium">{c.nombre}</td>
                <td className="px-4 py-3 text-gray-600">{c.descripcion || '-'}</td>
                <td className="px-4 py-3 text-center">
                  <button onClick={() => toggle(c)} className={`text-2xl ${c.activo ? 'text-green-600' : 'text-gray-400'}`}>
                    {c.activo ? <FaToggleOn /> : <FaToggleOff />}</button>
                </td>
                <td className="px-4 py-3"><div className="flex justify-end gap-3 text-lg">
                  <Link to={`/catalog/categories/${c.id}/edit`} className="text-primary-600"><FaEdit /></Link>
                  <button onClick={() => del(c.id)} className="text-red-600"><FaTrash /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-10 text-gray-500">No hay categorias</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default CategoryList;
