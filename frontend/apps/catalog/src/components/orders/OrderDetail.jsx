import { useState, useEffect } from 'react';
import ColorChips from '../common/ColorChips';
import DevolucionModal from './DevolucionModal';
import { useParams, useNavigate } from 'react-router-dom';
import { FaArrowLeft, FaCheck, FaBan, FaMoneyBillWave, FaPlay, FaUndo } from 'react-icons/fa';
import orderService from '../../services/orderService';

const estadoColor = {
  borrador: 'bg-gray-100 text-gray-800', confirmada: 'bg-primary-100 text-primary-800',
  en_proceso: 'bg-yellow-100 text-yellow-800', completada: 'bg-green-100 text-green-800',
  cancelada: 'bg-red-100 text-red-800',
};

const OrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [devoluciones, setDevoluciones] = useState([]);
  const [saldoFavor, setSaldoFavor] = useState(0);
  const [showDevolucion, setShowDevolucion] = useState(false);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pago, setPago] = useState({ monto: '', metodo: 'efectivo', referencia: '', observaciones: '', banco: '', fechaTransaccion: '', comprobanteUrl: '', ultimosDigitos: '' });

  useEffect(() => { load(); }, [id]);

  const load = async () => {
    try {
      setLoading(true);
      const [o, p, dv, sf] = await Promise.all([
        orderService.getById(id), orderService.getPayments(id),
        orderService.getDevoluciones(id).catch(() => []),
        orderService.saldoCliente(id).catch(() => ({ saldoAFavor: 0 })),
      ]);
      setOrder(o); setPayments(p); setDevoluciones(dv); setSaldoFavor(Number(sf.saldoAFavor || 0));
    } catch (e) { console.error(e); } finally { setLoading(false); }
  };

  const doAction = async (fn, ...args) => {
    setError('');
    try { await fn(id, ...args); load(); }
    catch (err) { setError(err.response?.data?.message || 'Error en la operacion'); }
  };

  const confirmar = () => { if (window.confirm('¿Confirmar la orden? Esto descuenta el inventario definitivamente.')) doAction(orderService.confirm); };
  const cancelar = () => { const m = window.prompt('Motivo de cancelacion:'); if (m !== null) doAction(orderService.cancel, m || 'Sin motivo'); };
  const reservar = () => { if (window.confirm('¿Marcar el pago en proceso? Se RESERVA (bloquea) el stock sin sacarlo; podrás reversar.')) doAction(orderService.reserve); };
  const reversar = () => { if (window.confirm('¿Reversar la orden? Se libera la reserva y vuelve a borrador.')) doAction(orderService.reverse); };

  const registrarPago = async (e) => {
    e.preventDefault(); setError('');
    try {
      await orderService.registerPayment(id, {
        monto: parseFloat(pago.monto), metodo: pago.metodo,
        referencia: pago.referencia || null, observaciones: pago.observaciones || null,
        banco: pago.banco || null, fechaTransaccion: pago.fechaTransaccion || null,
        comprobanteUrl: pago.comprobanteUrl || null, ultimosDigitos: pago.ultimosDigitos || null,
      });
      setPago({ monto: '', metodo: 'efectivo', referencia: '', observaciones: '', banco: '', fechaTransaccion: '', comprobanteUrl: '', ultimosDigitos: '' });
      load();
    } catch (err) { setError(err.response?.data?.message || 'Error al registrar pago'); }
  };

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  if (!order) return <div className="text-center py-10">Orden no encontrada</div>;

  const puedeReservar = order.estado === 'borrador';
  const puedeConfirmar = order.estado === 'borrador' || order.estado === 'en_proceso';
  const puedeReversar = order.estado === 'en_proceso';
  const puedePagar = ['confirmada', 'en_proceso'].includes(order.estado) && order.saldoPendiente > 0;
  const puedeCancelar = order.estado !== 'completada' && order.estado !== 'cancelada';

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/catalog/orders')} className="text-primary-600"><FaArrowLeft size={22} /></button>
        <h1 className="text-3xl font-bold">{order.numeroOrden}</h1>
        <span className={`px-3 py-1 rounded text-sm ${estadoColor[order.estado] || 'bg-gray-100'}`}>{order.estado}</span>
      </div>

      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <div className="flex gap-3 mb-6 flex-wrap">
        {puedeReservar && <button onClick={reservar} className="bg-yellow-500 hover:bg-yellow-600 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2"><FaPlay /> Pago en proceso (reservar)</button>}
        {puedeConfirmar && <button onClick={confirmar} className="btn-primary flex items-center gap-2"><FaCheck /> Confirmar</button>}
        {puedeReversar && <button onClick={reversar} className="bg-orange-500 hover:bg-orange-600 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2"><FaUndo /> Reversar</button>}
        {puedeCancelar && <button onClick={cancelar} className="btn-danger flex items-center gap-2"><FaBan /> Cancelar</button>}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <div className="card md:col-span-2">
          <h2 className="text-xl font-bold mb-4">Informacion</h2>
          <div className="grid grid-cols-2 gap-3">
            <p><strong>Cliente:</strong> {order.clienteNombre}</p>
            <p><strong>Fecha:</strong> {order.fechaOrden}</p>
            <p><strong>Sede:</strong> {order.sedeNombre || `#${order.sedeId}`}</p>
            <p><strong>Items:</strong> {order.detalles?.length || 0}</p>
          </div>
          {order.notas && <p className="mt-3 pt-3 border-t text-gray-600">{order.notas}</p>}
        </div>
        <div className="card">
          <h2 className="text-lg font-bold mb-3">Totales</h2>
          <div className="space-y-1 text-right">
            <p className="flex justify-between"><span className="text-gray-500">Subtotal:</span> <span className="font-mono">${(order.subtotal||0).toLocaleString()}</span></p>
            <p className="flex justify-between"><span className="text-gray-500">Descuento:</span> <span className="font-mono text-red-600">-${(order.descuento||0).toLocaleString()}</span></p>
            <p className="flex justify-between border-t pt-1"><span className="font-bold">Total:</span> <span className="font-mono font-bold">${(order.total||0).toLocaleString()}</span></p>
            {Number(order.montoDevuelto) > 0 && (
              <p className="flex justify-between"><span className="text-gray-500">Devuelto:</span> <span className="font-mono text-amber-700">-${Number(order.montoDevuelto).toLocaleString()}</span></p>
            )}
            <p className="flex justify-between"><span className="text-gray-500">Saldo:</span> <span className={`font-mono ${order.saldoPendiente > 0 ? 'text-red-600' : 'text-green-600'}`}>${(order.saldoPendiente||0).toLocaleString()}</span></p>
          </div>
        </div>
      </div>

      <div className="card mb-6 p-0 overflow-x-auto">
        <h2 className="text-xl font-bold p-6 pb-3">Detalle</h2>
        <table className="min-w-full">
          <thead className="bg-gray-50"><tr>
            <th className="px-4 py-3 text-left">Producto</th><th className="px-4 py-3 text-left">Unidad</th>
            <th className="px-4 py-3 text-center">Cant.</th><th className="px-4 py-3 text-right">P. Unit.</th>
            <th className="px-4 py-3 text-right">Desc.</th><th className="px-4 py-3 text-right">Subtotal</th>
          </tr></thead>
          <tbody>
            {order.detalles?.map(d => (
              <tr key={d.id} className="border-b">
                <td className="px-4 py-3">
                  {d.productoNombre}
                  {d.color && <div className="mt-1"><ColorChips colores={[d.color]} size="xs" /></div>}
                </td>
                <td className="px-4 py-3 font-mono text-sm">{d.unidadCodigo}</td>
                <td className="px-4 py-3 text-center">{d.cantidad}</td>
                <td className="px-4 py-3 text-right font-mono">${(d.precioUnitario||0).toLocaleString()}</td>
                <td className="px-4 py-3 text-right">${(d.descuentoLinea||0).toLocaleString()}</td>
                <td className="px-4 py-3 text-right font-mono">${(d.subtotal||0).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-xl font-bold mb-4 flex items-center gap-2"><FaMoneyBillWave className="text-green-600" /> Pagos</h2>
          {payments.length === 0 ? <p className="text-gray-500">Sin pagos registrados</p> : (
            <div className="space-y-2">
              {payments.map(p => (
                <div key={p.id} className="flex justify-between border-b pb-2">
                  <div>
                    <span className="font-mono font-semibold">${(p.monto||0).toLocaleString()}</span>
                    <span className="ml-2 px-2 py-1 bg-gray-100 rounded text-xs">{p.metodo}</span>
                  </div>
                  <span className="text-sm text-gray-500">{p.fechaPago ? new Date(p.fechaPago).toLocaleDateString() : ''}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {puedePagar && (
          <div className="card">
            <h2 className="text-xl font-bold mb-4">Registrar pago</h2>
            <form onSubmit={registrarPago} className="space-y-3">
              <div>
                <label className="block text-sm mb-1">Monto (saldo: ${(order.saldoPendiente||0).toLocaleString()})</label>
                <input type="number" step="0.01" max={order.saldoPendiente} className="input-field" value={pago.monto} onChange={e => setPago({...pago, monto: e.target.value})} required />
              </div>
              <div>
                <label className="block text-sm mb-1">Método de pago</label>
                <select className="input-field" value={pago.metodo} onChange={e => setPago({...pago, metodo: e.target.value})}>
                  <option value="efectivo">Efectivo</option>
                  <option value="transferencia">Transferencia</option>
                  <option value="tarjeta">Tarjeta débito</option>
                  <option value="credito">Tarjeta crédito</option>
                  {saldoFavor > 0 && <option value="nota_credito">Saldo a favor del cliente (${saldoFavor.toLocaleString()})</option>}
                </select>
              </div>

              {pago.metodo === 'transferencia' && (
                <>
                  <div><label className="block text-sm mb-1">Banco origen</label>
                    <input className="input-field" value={pago.banco} onChange={e => setPago({...pago, banco: e.target.value})} placeholder="Banco desde el que transfirieron" /></div>
                  <div><label className="block text-sm mb-1">N.º de transacción / comprobante</label>
                    <input className="input-field" value={pago.referencia} onChange={e => setPago({...pago, referencia: e.target.value})} /></div>
                </>
              )}
              {(pago.metodo === 'tarjeta' || pago.metodo === 'credito') && (
                <>
                  <div><label className="block text-sm mb-1">Últimos 4 dígitos</label>
                    <input maxLength="4" className="input-field font-mono" value={pago.ultimosDigitos} onChange={e => setPago({...pago, ultimosDigitos: e.target.value.replace(/[^0-9]/g,'')})} placeholder="1234" /></div>
                  <div><label className="block text-sm mb-1">Código de aprobación</label>
                    <input className="input-field" value={pago.referencia} onChange={e => setPago({...pago, referencia: e.target.value})} /></div>
                </>
              )}

              <div><label className="block text-sm mb-1">Fecha de la transacción</label>
                <input type="date" className="input-field" value={pago.fechaTransaccion} onChange={e => setPago({...pago, fechaTransaccion: e.target.value})} /></div>
              <div><label className="block text-sm mb-1">Soporte / comprobante (URL)</label>
                <input className="input-field" value={pago.comprobanteUrl} onChange={e => setPago({...pago, comprobanteUrl: e.target.value})} placeholder="Enlace a la imagen del soporte" /></div>
              <div><label className="block text-sm mb-1">Observaciones</label>
                <input className="input-field" value={pago.observaciones} onChange={e => setPago({...pago, observaciones: e.target.value})} /></div>

              <button type="submit" className="btn-primary w-full">Registrar pago</button>
            </form>
          </div>
        )}
      </div>
      {(order.estado === 'confirmada' || order.estado === 'completada' || devoluciones.length > 0) && (
        <div className="card mt-6">
          <div className="flex justify-between items-center mb-3">
            <h2 className="text-lg font-bold flex items-center gap-2"><FaUndo className="text-primary-600" /> Devoluciones</h2>
            {(order.estado === 'confirmada' || order.estado === 'completada') && (
              <button onClick={() => setShowDevolucion(true)} className="btn-secondary text-sm">Registrar devolución</button>
            )}
          </div>
          {devoluciones.length === 0 ? <p className="text-sm text-gray-500">Sin devoluciones.</p> : (
            <div className="divide-y">
              {devoluciones.map(dv => (
                <div key={dv.id} className="py-3 text-sm">
                  <div className="flex justify-between">
                    <span><strong>{dv.numero}</strong> · {dv.tipoMotivo.replace('_', ' ')} · {String(dv.createdAt || '').slice(0, 10)}</span>
                    <span className="font-mono font-semibold">${Number(dv.montoTotal).toLocaleString()}</span>
                  </div>
                  <p className="text-gray-600 mt-0.5">{dv.observacion}</p>
                  <p className="text-xs text-gray-500 mt-1">
                    {dv.lineas.map(l => `${l.productoNombre} ${l.unidadCodigo} ×${l.cantidad} (${({ disponible: 'vuelve a la venta', revision: 'a revisión', baja: 'dada de baja' })[l.destino] || l.destino})`).join(' · ')}
                  </p>
                  <p className="text-xs mt-1">
                    Redujo saldo: ${Number(dv.montoAplicadoSaldo).toLocaleString()}
                    {Number(dv.montoNotaCredito) > 0 && <> · Nota crédito ${Number(dv.montoNotaCredito).toLocaleString()} ({dv.tratamiento === 'reembolso' ? `reembolso ${dv.metodoReembolso || ''}` : 'saldo a favor'})</>}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
      {showDevolucion && <DevolucionModal order={order} devoluciones={devoluciones} onClose={() => setShowDevolucion(false)} onDone={() => { setShowDevolucion(false); load(); }} />}
    </div>
  );
};

export default OrderDetail;
