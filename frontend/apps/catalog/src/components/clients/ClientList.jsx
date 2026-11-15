import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaUsers, FaSearch } from 'react-icons/fa';
import clientService from '../../services/clientService';

const ClientList = () => {
  const [texto, setTexto] = useState('');
  const [q, setQ] = useState('');
  useEffect(() => { const t = setTimeout(() => setQ(texto.trim()), 350); return () => clearTimeout(t); }, [texto]);
  // Solo se trae la página visible
  const pag = usePaginacionServidor(({ page, size }) => clientService.paginado({ page, size, q }), [q], { clave: 'clientes' });
  const items = pag.items;
  const loading = pag.cargando && items.length === 0 && !q && !texto;
  const load = pag.recargar;
  const del = async (id) => { if(!window.confirm('¿Desactivar cliente?'))return; try{ await clientService.delete(id); load(); }catch(e){alert(e.response?.data?.message||'Error');} };

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaUsers className="text-primary-600" /> Clientes</h1>
        <Link to="/catalog/clients/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nuevo</Link>
      </div>
      <div className="relative mb-4 max-w-md">
        <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={13} />
        <input className="input-field pl-9" placeholder="Nombre, documento, correo o teléfono…" value={texto} onChange={e => setTexto(e.target.value)} />
      </div>
      <div className={`card p-0 overflow-x-auto transition-opacity ${pag.cargando ? 'opacity-60' : ''}`}>
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Nombre</th><th className="px-4 py-3 text-left">Tipo</th>
            <th className="px-4 py-3 text-left">Documento</th><th className="px-4 py-3 text-left">Contacto</th>
            <th className="px-4 py-3 text-right">Credito</th><th className="px-4 py-3 text-right">Acciones</th>
          </tr></thead>
          <tbody>
            {items.map(c => (
              <tr key={c.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">{c.nombre}</td>
                <td className="px-4 py-3"><span className={`px-2 py-1 rounded text-xs ${c.tipo === 'mayorista' ? 'bg-indigo-100 text-indigo-800' : 'bg-teal-100 text-teal-800'}`}>{c.tipo}</span></td>
                <td className="px-4 py-3">{c.documento || '-'}</td>
                <td className="px-4 py-3 text-sm">{c.email || c.telefono || '-'}</td>
                <td className="px-4 py-3 text-right font-mono">${(c.limiteCredito || 0).toLocaleString()}</td>
                <td className="px-4 py-3"><div className="flex justify-end gap-3 text-lg">
                  <Link to={`/catalog/clients/${c.id}/edit`} className="text-primary-600"><FaEdit /></Link>
                  <button onClick={() => del(c.id)} className="text-red-600"><FaTrash /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && !pag.cargando && <div className="text-center py-10 text-gray-500">{q ? 'No hay clientes que coincidan con la búsqueda' : 'No hay clientes'}</div>}
        <Paginacion {...pag.paginador} />
      </div>
    </div>
  );
};
export default ClientList;
