import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaUndo, FaPlus, FaSearch } from 'react-icons/fa';
import devolucionService from '../../services/devolucionService';

const money = (n) => '$' + Number(n || 0).toLocaleString('es-CO', { maximumFractionDigits: 0 });
const MOTIVO = { garantia: 'Garantía / defecto', cambio: 'Cambio', error_venta: 'Error en la venta', desistimiento: 'Desistimiento', otro: 'Otro' };

export default function DevolucionesList() {
  const [f, setF] = useState({ desde: '', hasta: '', q: '' });
  // La búsqueda de texto espera a que se deje de escribir
  const [q, setQ] = useState('');
  useEffect(() => { const t = setTimeout(() => setQ(f.q.trim()), 300); return () => clearTimeout(t); }, [f.q]);
  // Solo se trae la página visible
  const pag = usePaginacionServidor(({ page, size }) => devolucionService.paginado({ desde: f.desde, hasta: f.hasta, q, page, size }),
    [f.desde, f.hasta, q], { clave: 'devoluciones' });
  const items = pag.items;
  const loading = pag.cargando && items.length === 0;
  const error = pag.error;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaUndo className="text-primary-600" /> Devoluciones</h1>
        <Link to="/catalog/devoluciones/nueva" className="btn-primary flex items-center gap-2"><FaPlus /> Nueva devolución</Link>
      </div>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      <div className="card mb-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <div className="md:col-span-2 relative"><label className="block text-xs text-gray-500 mb-1">Factura o cliente</label>
          <FaSearch className="absolute left-3 bottom-3 text-gray-400" />
          <input className="input-field pl-9" placeholder="Número de factura, devolución, cliente o documento…" value={f.q} onChange={e => setF({ ...f, q: e.target.value })} /></div>
        <div><label className="block text-xs text-gray-500 mb-1">Desde</label><input type="date" className="input-field" value={f.desde} onChange={e => setF({ ...f, desde: e.target.value })} /></div>
        <div><label className="block text-xs text-gray-500 mb-1">Hasta</label><input type="date" className="input-field" value={f.hasta} onChange={e => setF({ ...f, hasta: e.target.value })} /></div>
      </div>
      <div className="card overflow-x-auto">
        {loading ? <div className="py-8 text-center text-gray-500">Cargando…</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b">
              <th className="py-2 px-3">Devolución</th><th className="py-2 px-3">Factura</th><th className="py-2 px-3">Cliente</th>
              <th className="py-2 px-3">Motivo</th><th className="py-2 px-3 text-right">Valor</th><th className="py-2 px-3">Tratamiento</th><th className="py-2 px-3">Fecha</th>
            </tr></thead>
            <tbody>
              {items.map(d => (
                <tr key={d.id} className="border-b hover:bg-gray-50">
                  <td className="py-2 px-3"><Link to={`/catalog/devoluciones/${d.id}`} className="text-primary-700 font-medium">{d.numero}</Link></td>
                  <td className="py-2 px-3"><Link to={`/catalog/orders/${d.ordenId}`} className="hover:text-primary-700">{d.ordenNumero}</Link></td>
                  <td className="py-2 px-3">{d.clienteNombre || <span className="text-gray-400">Ocasional</span>}</td>
                  <td className="py-2 px-3">{MOTIVO[d.tipoMotivo] || d.tipoMotivo}</td>
                  <td className="py-2 px-3 text-right font-mono">{money(d.montoTotal)}</td>
                  <td className="py-2 px-3 text-xs">{Number(d.montoNotaCredito) > 0
                    ? (d.tratamiento === 'reembolso' ? `Reembolso ${money(d.montoNotaCredito)}` : `Saldo a favor ${money(d.montoNotaCredito)}`)
                    : 'Abonado a la deuda'}</td>
                  <td className="py-2 px-3 text-xs text-gray-500">{String(d.createdAt || '').slice(0, 10)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="py-8 text-center text-gray-500">No hay devoluciones con esos filtros.</div>}
        <Paginacion {...pag.paginador} />
      </div>
    </div>
  );
}
