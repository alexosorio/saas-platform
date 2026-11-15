import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaToggleOn, FaToggleOff, FaDollarSign, FaTag } from 'react-icons/fa';
import listService from '../../services/listService';

const ListPriceList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { load(); }, []);
  const load = async () => { try { setLoading(true); setItems(await listService.getAll()); } catch(e){console.error(e);} finally { setLoading(false);} };
  const del = async (id) => { if(!window.confirm('¿Eliminar lista?'))return; try{ await listService.delete(id); load(); }catch(e){alert(e.response?.data?.message||'Error');} };
  const toggle = async (l) => { try{ l.activa?await listService.deactivate(l.id):await listService.activate(l.id); load(); }catch(e){alert(e.response?.data?.message||'Error');} };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'listas-precios' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaDollarSign className="text-primary-600" /> Listas de Precios</h1>
        <Link to="/catalog/lists/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva Lista</Link>
      </div>
      <p className="text-gray-500 mb-4">Define precios diferenciados por tipo de cliente (minorista / mayorista). Cada lista contiene precios por producto.</p>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {pg.visibles.map(l => (
          <div key={l.id} className="card">
            <div className="flex justify-between items-start mb-2">
              <h3 className="text-lg font-bold">{l.nombre}</h3>
              <button onClick={() => toggle(l)} className={`text-2xl ${l.activa ? 'text-green-600' : 'text-gray-400'}`}>{l.activa ? <FaToggleOn/> : <FaToggleOff/>}</button>
            </div>
            <span className={`px-2 py-1 rounded text-xs ${l.tipoCliente === 'mayorista' ? 'bg-indigo-100 text-indigo-800' : 'bg-teal-100 text-teal-800'}`}>{l.tipoCliente}</span>
            {l.descripcion && <p className="text-gray-600 text-sm mt-2">{l.descripcion}</p>}
            <div className="flex gap-3 mt-4 pt-3 border-t text-sm">
              <Link to={`/catalog/lists/${l.id}/prices`} className="text-purple-600 flex items-center gap-1"><FaTag /> Precios</Link>
              <Link to={`/catalog/lists/${l.id}/edit`} className="text-primary-600 flex items-center gap-1"><FaEdit /> Editar</Link>
              <button onClick={() => del(l.id)} className="text-red-600 flex items-center gap-1"><FaTrash /> Eliminar</button>
            </div>
          </div>
        ))}
      </div>
      {items.length === 0 && <div className="card text-center py-10 text-gray-500">No hay listas de precios</div>}
      {pg.total > 0 && <div className="card p-0 mt-4"><Paginacion {...pg.paginador} /></div>}
    </div>
  );
};
export default ListPriceList;
