import { useState, useEffect, useMemo } from 'react';
import { remoteOptions, Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import FiltrosCuentas, { FILTROS_VACIOS } from './FiltrosCuentas';
import clientService from '../../services/clientService';
import { Link } from 'react-router-dom';
import { FaHandHoldingUsd } from 'react-icons/fa';
import cuentaService from '../../services/cuentaService';

const money = (n) => n != null ? '$' + Number(n).toLocaleString() : '—';
const vence = (c) => {
  if (!c.fechaVencimiento) return <span className="text-gray-400">—</span>;
  const [y, m, d] = c.fechaVencimiento.split('-');
  return c.diasVencida > 0
    ? <span className="text-red-600 font-medium">{`${d}/${m}/${y}`} <span className="text-xs">({c.diasVencida} d vencida)</span></span>
    : <span>{`${d}/${m}/${y}`}</span>;
};
const badge = (e) => ({ pendiente: 'bg-red-100 text-red-700', parcial: 'bg-amber-100 text-amber-700', pagada: 'bg-green-100 text-green-700' }[e] || 'bg-gray-100 text-gray-600');

const CuentasPorCobrar = () => {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const terceroSrc = useMemo(() => remoteOptions((q) => clientService.search(q), { sublabel: 'documento' }), []);

  // Recarga al cambiar cualquier filtro (la etiqueta del tercero no cuenta). Solo se trae la página visible.
  const { terceroLabel, ...clave } = filtros;
  const pag = usePaginacionServidor(({ page, size }) => cuentaService.porCobrarPaginado({
    estado: filtros.estado, clienteId: filtros.terceroId, desde: filtros.desde, hasta: filtros.hasta,
    campoFecha: filtros.campoFecha, soloVencidas: filtros.soloVencidas, page, size,
  }), [clave], { clave: 'cuentasporcobrar' });
  const items = pag.items;
  const loading = pag.cargando && items.length === 0;
  const error = pag.error;
  const load = pag.recargar;
  // Total de TODO el filtro (no solo de la página), calculado en el servidor
  const totalPendiente = Number(pag.resumen?.saldoPendiente || 0);

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaHandHoldingUsd className="text-green-600" /> Cuentas por Cobrar</h1>
        <div className="text-right"><div className="text-xs text-gray-500">Por cobrar total</div><div className="text-xl font-bold text-green-600">{money(totalPendiente)}</div></div>
      </div>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      <FiltrosCuentas filtros={filtros} onChange={setFiltros} terceroSrc={terceroSrc} terceroLabel="Cliente" />
      <div className={`card overflow-x-auto p-0 transition-opacity ${pag.cargando ? 'opacity-60' : ''}`}>
        {loading ? <div className="py-8 text-center text-gray-500">Cargando...</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b">
              <th className="py-2 px-3">Orden</th><th className="py-2 px-3">Cliente</th>
              <th className="py-2 px-3">Vence</th><th className="py-2 px-3 text-right">Total</th><th className="py-2 px-3 text-right">Saldo</th><th className="py-2 px-3">Estado</th><th className="py-2 px-3"></th>
            </tr></thead>
            <tbody>
              {items.map(c => (
                <tr key={c.id} className="border-b hover:bg-gray-50">
                  <td className="py-2 px-3 font-mono text-xs">{c.ordenNumero || c.ordenId}</td>
                  <td className="py-2 px-3">{c.clienteNombre || '—'}</td>
                  <td className="py-2 px-3 whitespace-nowrap">{vence(c)}</td>
                  <td className="py-2 px-3 text-right">{money(c.monto)}</td>
                  <td className="py-2 px-3 text-right font-semibold">{money(c.saldo)}</td>
                  <td className="py-2 px-3"><span className={`text-xs px-2 py-0.5 rounded ${badge(c.estado)}`}>{c.estado}</span></td>
                  <td className="py-2 px-3 text-right">{c.ordenId && <Link to={`/catalog/orders/${c.ordenId}`} className="text-primary-600 text-sm">Ver orden</Link>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="py-8 text-center text-gray-500">No hay cuentas por cobrar.</div>}
        <Paginacion {...pag.paginador} />
      </div>
      <p className="text-xs text-gray-400 mt-2">Los abonos se registran desde el detalle de la orden (Registrar pago); aquí se refleja el saldo.</p>
    </div>
  );
};
export default CuentasPorCobrar;
