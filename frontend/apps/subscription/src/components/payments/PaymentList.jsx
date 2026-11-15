import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { FaMoneyBillWave, FaPlus } from 'react-icons/fa';
import paymentService from '../../services/paymentService';
import paymentMethodService from '../../services/paymentMethodService';

const estadoColor = {
  pendiente: 'bg-yellow-100 text-yellow-800', completado: 'bg-green-100 text-green-800',
  fallido: 'bg-red-100 text-red-800', reembolsado: 'bg-blue-100 text-blue-800', cancelado: 'bg-gray-100 text-gray-800',
};

const PaymentList = () => {
  const [items, setItems] = useState([]);
  const [methods, setMethods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ monto: '', moneda: 'USD', metodo: 'transferencia', tipo: 'suscripcion', descripcion: '', referenciaExterna: '', banco: '', ultimosDigitos: '', comprobanteUrl: '', fechaTransaccion: '' });

  useEffect(() => { load(); }, []);
  const load = async () => {
    try {
      setLoading(true);
      const [p, m] = await Promise.all([paymentService.getAll(), paymentMethodService.getAll()]);
      setItems(p); setMethods(m);
    } catch (e) { console.error(e); } finally { setLoading(false); }
  };

  const submit = async (e) => {
    e.preventDefault(); setError('');
    try {
      await paymentService.register({
        monto: parseFloat(form.monto), moneda: form.moneda,
        metodo: form.metodo, banco: form.banco || null,
        ultimosDigitos: form.ultimosDigitos || null, comprobanteUrl: form.comprobanteUrl || null,
        fechaTransaccion: form.fechaTransaccion || null,
        tipo: form.tipo, descripcion: form.descripcion || null,
        referenciaExterna: form.referenciaExterna || null,
      });
      setShowForm(false);
      setForm({ monto: '', moneda: 'USD', metodo: 'transferencia', tipo: 'suscripcion', descripcion: '', referenciaExterna: '', banco: '', ultimosDigitos: '', comprobanteUrl: '', fechaTransaccion: '' });
      load();
    } catch (err) { setError(err.response?.data?.message || 'Error'); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'pagos' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaMoneyBillWave className="text-green-600" /> Pagos</h1>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary-700 hover:bg-primary-800 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2">
          <FaPlus /> {showForm ? 'Cancelar' : 'Registrar pago'}
        </button>
      </div>

      {showForm && (
        <div className="card mb-6">
          {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
          <p className="text-sm text-primary-800 bg-primary-50 px-3 py-2 rounded mb-3">Los pagos que registres quedan <strong>en validación</strong> hasta que verifiquemos el soporte. Tu suscripción se activa al aprobarse el pago.</p>
          <form onSubmit={submit} className="grid grid-cols-2 gap-4">
            <div><label className="block font-semibold mb-2">Monto *</label>
              <input type="number" step="0.01" className="input-field" value={form.monto} onChange={e => setForm({...form, monto: e.target.value})} required /></div>
            <div><label className="block font-semibold mb-2">Moneda</label>
              <input className="input-field" value={form.moneda} onChange={e => setForm({...form, moneda: e.target.value.toUpperCase()})} maxLength="3" /></div>
            <div><label className="block font-semibold mb-2">Método de pago</label>
              <select className="input-field" value={form.metodo} onChange={e => setForm({...form, metodo: e.target.value})}>
                <option value="transferencia">Transferencia</option>
                <option value="tarjeta_credito">Tarjeta crédito</option>
                <option value="tarjeta_debito">Tarjeta débito</option>
                <option value="efectivo">Efectivo</option>
              </select></div>
            <div><label className="block font-semibold mb-2">Fecha de la transacción</label>
              <input type="date" className="input-field" value={form.fechaTransaccion} onChange={e => setForm({...form, fechaTransaccion: e.target.value})} /></div>

            {form.metodo === 'transferencia' && (
              <div className="col-span-2 grid grid-cols-2 gap-4">
                <div><label className="block font-semibold mb-2">Banco origen</label>
                  <input className="input-field" value={form.banco} onChange={e => setForm({...form, banco: e.target.value})} /></div>
                <div><label className="block font-semibold mb-2">N.º de transacción / comprobante</label>
                  <input className="input-field" value={form.referenciaExterna} onChange={e => setForm({...form, referenciaExterna: e.target.value})} /></div>
              </div>
            )}
            {(form.metodo === 'tarjeta_credito' || form.metodo === 'tarjeta_debito') && (
              <div className="col-span-2 grid grid-cols-2 gap-4">
                <div><label className="block font-semibold mb-2">Últimos 4 dígitos</label>
                  <input maxLength="4" className="input-field font-mono" value={form.ultimosDigitos} onChange={e => setForm({...form, ultimosDigitos: e.target.value.replace(/[^0-9]/g,'')})} /></div>
                <div><label className="block font-semibold mb-2">Código de aprobación</label>
                  <input className="input-field" value={form.referenciaExterna} onChange={e => setForm({...form, referenciaExterna: e.target.value})} /></div>
              </div>
            )}
            <div className="col-span-2"><label className="block font-semibold mb-2">Soporte / comprobante (URL)</label>
              <input className="input-field" value={form.comprobanteUrl} onChange={e => setForm({...form, comprobanteUrl: e.target.value})} placeholder="Enlace a la imagen del soporte enviado" /></div>
            <div><label className="block font-semibold mb-2">Tipo</label>
              <select className="input-field" value={form.tipo} onChange={e => setForm({...form, tipo: e.target.value})}>
                <option value="suscripcion">Suscripcion</option><option value="upgrade">Upgrade</option><option value="adicional">Adicional</option>
              </select></div>
            <div className="col-span-2"><label className="block font-semibold mb-2">Descripcion</label>
              <input className="input-field" value={form.descripcion} onChange={e => setForm({...form, descripcion: e.target.value})} /></div>
            <div className="col-span-2">
              <button type="submit" className="bg-primary-700 hover:bg-primary-800 text-white font-medium py-2 px-4 rounded-lg">Registrar</button>
            </div>
          </form>
        </div>
      )}

      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Referencia</th>
            <th className="px-4 py-3 text-right">Monto</th>
            <th className="px-4 py-3 text-left">Tipo</th>
            <th className="px-4 py-3 text-left">Estado</th>
            <th className="px-4 py-3 text-left">Fecha pago</th>
            <th className="px-4 py-3 text-left">Descripcion</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(p => (
              <tr key={p.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-sm">{p.referenciaInterna}</td>
                <td className="px-4 py-3 text-right font-mono">${(p.monto || 0).toLocaleString()} {p.moneda}</td>
                <td className="px-4 py-3"><span className="px-2 py-1 bg-primary-100 text-primary-800 rounded text-xs">{p.tipo}</span></td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-1 rounded text-xs ${estadoColor[p.estado] || 'bg-gray-100'}`}>
                    {({ pendiente: 'En validación', completado: 'Aprobado', fallido: 'Rechazado' })[p.estado] || p.estado}
                  </span>
                  {p.motivoRechazo && <div className="text-xs text-red-600 mt-1">Motivo: {p.motivoRechazo}</div>}
                </td>
                <td className="px-4 py-3 text-sm">{p.fechaPago || '-'}</td>
                <td className="px-4 py-3 text-sm">{p.descripcion || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-10 text-gray-500">No hay pagos registrados</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default PaymentList;
