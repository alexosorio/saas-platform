import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaListUl } from 'react-icons/fa';
import characteristicService from '../../services/characteristicService';

const CharacteristicList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { load(); }, []);
  const load = async () => { try { setLoading(true); setItems(await characteristicService.getAll()); } catch(e){console.error(e);} finally { setLoading(false);} };
  const del = async (id) => { if(!window.confirm('¿Eliminar caracteristica?'))return; try{ await characteristicService.delete(id); load(); }catch(e){alert(e.response?.data?.message||'Error');} };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'caracteristicas' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaListUl className="text-primary-600" /> Caracteristicas</h1>
        <Link to="/catalog/characteristics/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva</Link>
      </div>
      <p className="text-gray-500 mb-4">Atributos dinamicos que luego se asignan a las unidades (ej: Color, Almacenamiento, RAM).</p>
      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Codigo</th><th className="px-4 py-3 text-left">Nombre</th>
            <th className="px-4 py-3 text-left">Tipo</th><th className="px-4 py-3 text-left">Aplica a</th>
            <th className="px-4 py-3 text-center">Obligatorio</th><th className="px-4 py-3 text-right">Acciones</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(c => (
              <tr key={c.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-sm">{c.codigo}</td>
                <td className="px-4 py-3 font-medium">{c.nombre}</td>
                <td className="px-4 py-3"><span className="px-2 py-1 bg-purple-100 text-purple-800 rounded text-xs">{c.tipoDato}</span></td>
                <td className="px-4 py-3">{c.aplicableA}</td>
                <td className="px-4 py-3 text-center">{c.obligatorio ? '✓' : '-'}</td>
                <td className="px-4 py-3"><div className="flex justify-end gap-3 text-lg">
                  <Link to={`/catalog/characteristics/${c.id}/edit`} className="text-primary-600"><FaEdit /></Link>
                  <button onClick={() => del(c.id)} className="text-red-600"><FaTrash /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-10 text-gray-500">No hay caracteristicas</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default CharacteristicList;
