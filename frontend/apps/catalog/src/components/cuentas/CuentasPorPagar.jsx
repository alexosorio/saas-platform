import { useState, useEffect, useMemo } from 'react';
import { remoteOptions, Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import FiltrosCuentas, { FILTROS_VACIOS } from './FiltrosCuentas';
import supplierService from '../../services/supplierService';
import { FaMoneyBillWave, FaFileUpload, FaTimes } from 'react-icons/fa';
import cuentaService from '../../services/cuentaService';

const money = (n) => n != null ? '$' + Number(n).toLocaleString() : '—';
const vence = (c) => {
  if (!c.fechaVencimiento) return <span className="text-gray-400">—</span>;
  const [y, m, d] = c.fechaVencimiento.split('-');
  return c.diasVencida > 0
    ? <span className="text-red-600 font-medium">{`${d}/${m}/${y}`} <span className="text-xs">({c.diasVencida} d vencida)</span></span>
    : <span>{`${d}/${m}/${y}`}</span>;
};
const badge = (e) => ({ pendiente: 'bg-red-100 text-red-700', parcial: 'bg-amber-100 text-amber-700', pagada: 'bg-green-100 text-green-700',
  anulada: 'bg-gray-200 text-gray-600', saldo_a_favor: 'bg-primary-100 text-primary-700' }[e] || 'bg-gray-100 text-gray-600');
const ETIQUETA = { pendiente: 'pendiente', parcial: 'parcial', pagada: 'pagada', anulada: 'anulada', saldo_a_favor: 'saldo a favor' };

const CuentasPorPagar = () => {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const terceroSrc = useMemo(() => remoteOptions((q) => supplierService.search(q), { sublabel: 'email' }), []);
  const [pagar, setPagar] = useState(null); // cuenta seleccionada para pagar

  // Recarga al cambiar cualquier filtro (la etiqueta del tercero no cuenta). Solo se trae la página visible.
  const { terceroLabel, ...clave } = filtros;
  const pag = usePaginacionServidor(({ page, size }) => cuentaService.porPagarPaginado({
    estado: filtros.estado, proveedorId: filtros.terceroId, desde: filtros.desde, hasta: filtros.hasta,
    campoFecha: filtros.campoFecha, soloVencidas: filtros.soloVencidas, page, size,
  }), [clave], { clave: 'cuentasporpagar' });
  const items = pag.items;
  const loading = pag.cargando && items.length === 0;
  const error = pag.error;
  const load = pag.recargar;

  // Total de TODO el filtro (no solo de la página), calculado en el servidor
  const totalPendiente = Number(pag.resumen?.saldoPendiente || 0);

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaMoneyBillWave className="text-red-600" /> Cuentas por Pagar</h1>
        <div className="text-right"><div className="text-xs text-gray-500">Saldo pendiente total</div><div className="text-xl font-bold text-red-600">{money(totalPendiente)}</div></div>
      </div>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      <FiltrosCuentas filtros={filtros} onChange={setFiltros} terceroSrc={terceroSrc} terceroLabel="Proveedor" />
      <div className={`card overflow-x-auto p-0 transition-opacity ${pag.cargando ? 'opacity-60' : ''}`}>
        {loading ? <div className="py-8 text-center text-gray-500">Cargando...</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b">
              <th className="py-2 px-3">Unidad</th><th className="py-2 px-3">Producto</th><th className="py-2 px-3">Proveedor</th>
              <th className="py-2 px-3">Vence</th><th className="py-2 px-3 text-right">Monto</th><th className="py-2 px-3 text-right">Saldo</th><th className="py-2 px-3">Estado</th><th className="py-2 px-3"></th>
            </tr></thead>
            <tbody>
              {items.map(c => (
                <tr key={c.id} className="border-b hover:bg-gray-50">
                  <td className="py-2 px-3 font-mono text-xs">{c.unidadCodigo || '—'}</td>
                  <td className="py-2 px-3">{c.productoNombre || '—'}</td>
                  <td className="py-2 px-3">{c.proveedorNombre || <span className="text-gray-400">Sin proveedor</span>}
                    {c.notas?.map((n, i) => (
                      <div key={i} className="text-xs text-primary-700 mt-1 max-w-xs" title={n.motivo}>
                        Nota crédito {money(n.monto)}: {n.motivo}
                      </div>
                    ))}
                  </td>
                  <td className="py-2 px-3 whitespace-nowrap">{vence(c)}</td>
                  <td className="py-2 px-3 text-right">{money(c.monto)}</td>
                  <td className="py-2 px-3 text-right font-semibold">
                    {Number(c.saldo) < 0 ? <span className="text-primary-700" title="El proveedor nos debe este valor">A favor {money(-Number(c.saldo))}</span> : money(c.saldo)}
                  </td>
                  <td className="py-2 px-3"><span className={`text-xs px-2 py-0.5 rounded whitespace-nowrap ${badge(c.estado)}`}>{ETIQUETA[c.estado] || c.estado}</span></td>
                  <td className="py-2 px-3 text-right">
                    {Number(c.saldo) > 0 && <button onClick={() => setPagar(c)} className="text-primary-600 text-sm font-medium">Registrar pago</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="py-8 text-center text-gray-500">No hay cuentas por pagar.</div>}
        <Paginacion {...pag.paginador} />
      </div>
      {pagar && <PagoModal cuenta={pagar} onClose={() => setPagar(null)} onDone={() => { setPagar(null); load(); }} />}
    </div>
  );
};

const PagoModal = ({ cuenta, onClose, onDone }) => {
  const [f, setF] = useState({ monto: cuenta.saldo, metodo: 'transferencia', banco: '', referencia: '', fechaTransaccion: '', comprobanteUrl: '', ultimosDigitos: '', observaciones: '' });
  const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  const submit = async () => {
    setError(''); setSaving(true);
    try { await cuentaService.registrarPago(cuenta.id, { ...f, monto: parseFloat(f.monto) }); onDone(); }
    catch (e) { setError(e.response?.data?.message || 'Error'); } finally { setSaving(false); }
  };
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex justify-between items-center px-5 py-3 border-b">
          <h3 className="font-bold flex items-center gap-2"><FaFileUpload className="text-primary-600" /> Pago a proveedor</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700"><FaTimes /></button>
        </div>
        <div className="p-5 space-y-3">
          {error && <div className="bg-red-100 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          <div className="text-sm text-gray-500">Saldo: <b>{money(cuenta.saldo)}</b>{cuenta.proveedorNombre && <> · {cuenta.proveedorNombre}</>}</div>
          <div><label className="block text-sm mb-1">Monto</label><input type="number" step="0.01" max={cuenta.saldo} className="input-field" value={f.monto} onChange={e => setF({ ...f, monto: e.target.value })} /></div>
          <div><label className="block text-sm mb-1">Método</label>
            <select className="input-field" value={f.metodo} onChange={e => setF({ ...f, metodo: e.target.value })}>
              <option value="transferencia">Transferencia</option><option value="efectivo">Efectivo</option><option value="tarjeta">Tarjeta</option>
            </select></div>
          {f.metodo === 'transferencia' && (
            <><div><label className="block text-sm mb-1">Banco</label><input className="input-field" value={f.banco} onChange={e => setF({ ...f, banco: e.target.value })} /></div>
            <div><label className="block text-sm mb-1">N.º transacción / referencia</label><input className="input-field" value={f.referencia} onChange={e => setF({ ...f, referencia: e.target.value })} /></div></>
          )}
          <div><label className="block text-sm mb-1">Fecha de la transferencia</label><input type="date" className="input-field" value={f.fechaTransaccion} onChange={e => setF({ ...f, fechaTransaccion: e.target.value })} /></div>
          <div><label className="block text-sm mb-1">Soporte (URL del comprobante)</label><input className="input-field" value={f.comprobanteUrl} onChange={e => setF({ ...f, comprobanteUrl: e.target.value })} placeholder="Enlace a la imagen de la transferencia" /></div>
          <div className="flex gap-2 pt-2">
            <button onClick={submit} disabled={saving} className="btn-primary flex-1">{saving ? 'Guardando...' : 'Registrar pago'}</button>
            <button onClick={onClose} className="btn-secondary">Cancelar</button>
          </div>
        </div>
      </div>
    </div>
  );
};
export default CuentasPorPagar;
