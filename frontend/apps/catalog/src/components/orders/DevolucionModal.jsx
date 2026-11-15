import { useState, useMemo } from 'react';
import { FaTimes } from 'react-icons/fa';
import orderService from '../../services/orderService';

const MOTIVOS = [
  ['garantia', 'Garantía / defecto'], ['cambio', 'Cambio de producto'], ['error_venta', 'Error en la venta'],
  ['desistimiento', 'Desistimiento del cliente'], ['otro', 'Otro'],
];
const DESTINOS = [
  ['disponible', 'Vuelve a la venta'], ['revision', 'A revisión / reparación'], ['baja', 'Inservible (dar de baja)'],
];
const money = (n) => '$' + Number(n || 0).toLocaleString('es-CO', { maximumFractionDigits: 0 });

/** Devolución de venta: reingreso al inventario + nota crédito al cliente. */
export default function DevolucionModal({ order, devoluciones, onClose, onDone }) {
  // Cantidad ya devuelta por línea
  const devuelto = useMemo(() => {
    const m = {};
    devoluciones.forEach(d => d.lineas.forEach(l => { m[l.detalleOrdenId] = (m[l.detalleOrdenId] || 0) + l.cantidad; }));
    return m;
  }, [devoluciones]);
  const lineas = (order.detalles || []).map(d => ({ ...d, max: (d.cantidad || 1) - (devuelto[d.id] || 0) })).filter(d => d.max > 0);

  const [sel, setSel] = useState({});   // detalleId -> { cantidad, destino }
  const [motivo, setMotivo] = useState('');
  const [obs, setObs] = useState('');
  const [tratamiento, setTratamiento] = useState('');
  const [metodo, setMetodo] = useState('efectivo');
  const [referencia, setReferencia] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Estimación: prorrateo de descuento/impuestos globales (el servidor calcula el valor exacto)
  const sumaLineas = (order.detalles || []).reduce((s, d) => s + Number(d.subtotal || 0), 0);
  const factor = sumaLineas > 0 ? Number(order.total) / sumaLineas : 1;
  const credito = Object.entries(sel).reduce((s, [id, v]) => {
    const d = lineas.find(l => String(l.id) === id);
    return d ? s + (Number(d.subtotal) / (d.cantidad || 1)) * v.cantidad * factor : s;
  }, 0);
  const aplicadoSaldo = Math.min(credito, Number(order.saldoPendiente || 0));
  const excedente = Math.max(0, credito - aplicadoSaldo);

  const toggle = (d) => setSel(s => {
    const n = { ...s };
    if (n[d.id]) delete n[d.id]; else n[d.id] = { cantidad: d.max === 1 ? 1 : 1, destino: 'disponible' };
    return n;
  });

  const confirmar = async () => {
    setError('');
    if (!Object.keys(sel).length) return setError('Selecciona al menos una unidad');
    if (!motivo) return setError('Elige el motivo de la devolución');
    if (obs.trim().length < 10) return setError('Describe la devolución (mínimo 10 caracteres)');
    if (excedente > 0.5 && !tratamiento) return setError('Indica si el excedente se reembolsa o queda como saldo a favor');
    setEnviando(true);
    try {
      const resultado = await orderService.devolver(order.id, {
        tipoMotivo: motivo, observacion: obs.trim(),
        tratamiento: excedente > 0.5 ? tratamiento : null,
        metodoReembolso: tratamiento === 'reembolso' ? metodo : null,
        referenciaReembolso: tratamiento === 'reembolso' ? referencia : null,
        items: Object.entries(sel).map(([detalleId, v]) => ({ detalleId: Number(detalleId), cantidad: v.cantidad, destino: v.destino })),
      });
      onDone(resultado);
    } catch (e) { setError(e.response?.data?.message || 'No se pudo registrar la devolución'); setEnviando(false); }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[92vh] overflow-y-auto">
        <div className="flex justify-between items-center px-5 py-3 border-b sticky top-0 bg-white">
          <h3 className="font-bold">Devolución · Orden {order.numeroOrden}</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700" aria-label="Cerrar"><FaTimes /></button>
        </div>
        <div className="p-5 space-y-4">
          {error && <div className="bg-red-100 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          {lineas.length === 0 && <p className="text-sm text-gray-500">Todas las unidades de esta orden ya fueron devueltas.</p>}
          {lineas.length > 0 && (
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Selecciona lo que el cliente devuelve:</span>
              <button type="button" className="text-sm text-primary-700 font-medium"
                onClick={() => setSel(Object.fromEntries(lineas.map(d => [d.id, { cantidad: d.max, destino: 'disponible' }])))}>
                Devolver toda la factura
              </button>
            </div>
          )}

          <div className="space-y-2">
            {lineas.map(d => {
              const v = sel[d.id];
              return (
                <div key={d.id} className={`border rounded-lg p-3 ${v ? 'border-primary-400 bg-primary-50' : 'border-gray-200'}`}>
                  <label className="flex items-center gap-3 cursor-pointer">
                    <input type="checkbox" checked={!!v} onChange={() => toggle(d)} />
                    <span className="flex-1 text-sm"><strong>{d.productoNombre}</strong>
                      <span className="font-mono text-xs text-gray-500 ml-2">{d.unidadCodigo}</span>
                      {d.color && <span className="text-xs text-gray-500 ml-2">· {d.color}</span>}</span>
                    <span className="text-sm font-mono">{money(d.subtotal)}</span>
                  </label>
                  {v && (
                    <div className="grid grid-cols-2 gap-3 mt-2 pl-7">
                      {d.max > 1 ? (
                        <div><label className="block text-xs text-gray-500 mb-1">Cantidad (máx. {d.max})</label>
                          <input type="number" min={1} max={d.max} className="input-field input-sm" value={v.cantidad}
                            onChange={e => setSel(s => ({ ...s, [d.id]: { ...v, cantidad: Math.max(1, Math.min(d.max, Number(e.target.value) || 1)) } }))} /></div>
                      ) : <div />}
                      <div><label className="block text-xs text-gray-500 mb-1">¿En qué estado regresa?</label>
                        <select className="input-field input-sm" value={v.destino} onChange={e => setSel(s => ({ ...s, [d.id]: { ...v, destino: e.target.value } }))}>
                          {DESTINOS.map(([k, t]) => <option key={k} value={k}>{t}</option>)}
                        </select></div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div><label className="block text-sm font-semibold mb-1">Motivo *</label>
              <select className="input-field" value={motivo} onChange={e => setMotivo(e.target.value)}>
                <option value="">Seleccionar…</option>{MOTIVOS.map(([k, t]) => <option key={k} value={k}>{t}</option>)}
              </select></div>
            <div className="md:col-span-2"><label className="block text-sm font-semibold mb-1">Observación *</label>
              <input className="input-field" value={obs} onChange={e => setObs(e.target.value)} placeholder="Ej: La pantalla presenta líneas al encender." /></div>
          </div>

          {credito > 0 && (
            <div className="bg-gray-50 rounded-lg p-3 text-sm space-y-1">
              <div className="flex justify-between"><span>Valor a acreditar (aprox.)</span><strong>{money(credito)}</strong></div>
              <div className="flex justify-between text-gray-600"><span>Reduce lo que el cliente debe</span><span>{money(aplicadoSaldo)}</span></div>
              <div className="flex justify-between text-gray-600"><span>Excedente ya pagado → nota crédito</span><span>{money(excedente)}</span></div>
              <p className="text-xs text-gray-400">Incluye proporcionalmente el descuento y los impuestos de la orden. El valor exacto lo calcula el sistema.</p>
            </div>
          )}

          {excedente > 0.5 && (
            <div className="border rounded-lg p-3 space-y-2">
              <p className="text-sm font-semibold">¿Qué se hace con el excedente de {money(excedente)}?</p>
              <label className="flex items-center gap-2 text-sm"><input type="radio" checked={tratamiento === 'reembolso'} onChange={() => setTratamiento('reembolso')} /> Reembolsarlo al cliente</label>
              {tratamiento === 'reembolso' && (
                <div className="grid grid-cols-2 gap-3 pl-6">
                  <select className="input-field input-sm" value={metodo} onChange={e => setMetodo(e.target.value)}>
                    <option value="efectivo">Efectivo</option><option value="transferencia">Transferencia</option><option value="tarjeta">Reverso a tarjeta</option>
                  </select>
                  <input className="input-field input-sm" placeholder="Referencia (opcional)" value={referencia} onChange={e => setReferencia(e.target.value)} />
                </div>
              )}
              <label className={`flex items-center gap-2 text-sm ${!order.clienteId ? 'opacity-50' : ''}`}>
                <input type="radio" disabled={!order.clienteId} checked={tratamiento === 'saldo_a_favor'} onChange={() => setTratamiento('saldo_a_favor')} />
                Dejarlo como saldo a favor para compras futuras {!order.clienteId && '(la orden no tiene cliente)'}
              </label>
            </div>
          )}

          <div className="flex gap-2 pt-1">
            <button onClick={confirmar} disabled={enviando || !lineas.length} className="btn-primary flex-1 disabled:opacity-50">
              {enviando ? 'Registrando…' : 'Registrar devolución'}
            </button>
            <button onClick={onClose} className="btn-secondary">Cancelar</button>
          </div>
        </div>
      </div>
    </div>
  );
}
