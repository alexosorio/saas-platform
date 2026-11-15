import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaPlus, FaEye, FaFileInvoiceDollar, FaSearch } from 'react-icons/fa';
import orderService from '../../services/orderService';

const estadoColor = {
  borrador: 'bg-gray-100 text-gray-800', confirmada: 'bg-primary-100 text-primary-800',
  en_proceso: 'bg-yellow-100 text-yellow-800', completada: 'bg-green-100 text-green-800',
  cancelada: 'bg-red-100 text-red-800',
};
const estados = ['', 'borrador', 'confirmada', 'en_proceso', 'completada', 'cancelada'];

const OrderList = () => {
  const [filter, setFilter] = useState('');
  const [texto, setTexto] = useState('');
  const [q, setQ] = useState('');
  // Búsqueda con una pequeña espera para no consultar en cada tecla
  useEffect(() => { const t = setTimeout(() => setQ(texto.trim()), 350); return () => clearTimeout(t); }, [texto]);

  // Solo se trae la página visible
  const p = usePaginacionServidor(({ page, size }) => orderService.paginado({ page, size, estado: filter, q }),
    [filter, q], { clave: 'ordenes' });
  const orders = p.items;
  const loading = p.cargando && orders.length === 0;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaFileInvoiceDollar className="text-primary-600" /> Ordenes</h1>
        <Link to="/catalog/orders/new" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva Orden</Link>
      </div>

      <div className="flex gap-2 mb-4 flex-wrap items-center">
        <div className="relative w-full sm:w-72 sm:mr-2">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={13} />
          <input className="input-field pl-9" placeholder="N° de orden, cliente o documento…" value={texto} onChange={e => setTexto(e.target.value)} />
        </div>
        {estados.map(e => (
          <button key={e || 'todas'} onClick={() => setFilter(e)}
            className={`px-3 py-1 rounded-full text-sm ${filter === e ? 'bg-primary-600 text-white' : 'bg-gray-200 text-gray-700'}`}>
            {e === '' ? 'Todas' : e}
          </button>
        ))}
      </div>

      {loading ? <div className="text-center py-10 text-gray-500">Cargando...</div> : (
        <div className={`card p-0 overflow-x-auto transition-opacity ${p.cargando ? 'opacity-60' : ''}`}>
          <table className="min-w-full">
            <thead className="bg-gray-100"><tr>
              <th className="px-4 py-3 text-left">N° Orden</th><th className="px-4 py-3 text-left">Cliente</th>
              <th className="px-4 py-3 text-left">Fecha</th><th className="px-4 py-3 text-right">Total</th>
              <th className="px-4 py-3 text-right">Saldo</th><th className="px-4 py-3 text-center">Estado</th>
              <th className="px-4 py-3 text-right">Acciones</th>
            </tr></thead>
            <tbody>
              {orders.map(o => (
                <tr key={o.id} className="border-b hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-sm">{o.numeroOrden}</td>
                  <td className="px-4 py-3 font-medium">{o.clienteNombre}</td>
                  <td className="px-4 py-3 text-sm">{o.fechaOrden}</td>
                  <td className="px-4 py-3 text-right font-mono">${(o.total || 0).toLocaleString()}</td>
                  <td className="px-4 py-3 text-right font-mono">
                    <span className={o.saldoPendiente > 0 ? 'text-red-600' : 'text-green-600'}>${(o.saldoPendiente || 0).toLocaleString()}</span>
                  </td>
                  <td className="px-4 py-3 text-center"><span className={`px-2 py-1 rounded text-xs ${estadoColor[o.estado] || 'bg-gray-100'}`}>{o.estado}</span></td>
                  <td className="px-4 py-3 text-right">
                    <Link to={`/catalog/orders/${o.id}`} className="text-purple-600 inline-flex items-center gap-1"><FaEye /> Ver</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {orders.length === 0 && <div className="text-center py-10 text-gray-500">{p.error || (q ? 'No hay órdenes que coincidan con la búsqueda' : 'No hay ordenes')}</div>}
          <Paginacion {...p.paginador} />
        </div>
      )}
    </div>
  );
};
export default OrderList;
