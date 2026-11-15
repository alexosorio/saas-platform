import { useState, useEffect } from 'react';
import { FaCreditCard, FaPlus, FaTrash, FaStar, FaRegStar } from 'react-icons/fa';
import paymentMethodService from '../../services/paymentMethodService';

const tipoLabels = {
  tarjeta_credito: 'Tarjeta de credito',
  tarjeta_debito: 'Tarjeta de debito',
  transferencia: 'Transferencia',
  paypal: 'PayPal',
  stripe: 'Stripe',
};

const PaymentMethodList = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    tipo: 'tarjeta_credito', proveedor: '', ultimosDigitos: '', nombreTitular: '', fechaExpiracion: '', predeterminado: false,
    autorizacionDatos: false,
  });

  useEffect(() => { load(); }, []);
  const load = async () => {
    try { setLoading(true); setItems(await paymentMethodService.getAll()); }
    catch (e) { console.error(e); } finally { setLoading(false); }
  };

  const submit = async (e) => {
    e.preventDefault(); setError('');
    if (!form.autorizacionDatos) {
      setError('Debes autorizar el tratamiento de tus datos personales (habeas data) para continuar.');
      return;
    }
    try {
      await paymentMethodService.create({
        ...form,
        fechaExpiracion: form.fechaExpiracion || null,
        autorizacionDatos: true,
        fechaAutorizacion: new Date().toISOString(),
        versionPolitica: '1.0',
      });
      setShowForm(false);
      setForm({ tipo: 'tarjeta_credito', proveedor: '', ultimosDigitos: '', nombreTitular: '', fechaExpiracion: '', predeterminado: false, autorizacionDatos: false });
      load();
    } catch (err) { setError(err.response?.data?.message || 'Error'); }
  };

  const del = async (id) => {
    if (!window.confirm('¿Eliminar metodo de pago?')) return;
    try { await paymentMethodService.delete(id); load(); } catch (e) { alert(e.response?.data?.message || 'Error'); }
  };
  const setDefault = async (id) => {
    try { await paymentMethodService.markDefault(id); load(); } catch (e) { alert(e.response?.data?.message || 'Error'); }
  };

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaCreditCard className="text-primary-700" /> Metodos de Pago</h1>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary-700 hover:bg-primary-800 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2">
          <FaPlus /> {showForm ? 'Cancelar' : 'Agregar'}
        </button>
      </div>

      {showForm && (
        <div className="card mb-6">
          {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
          <form onSubmit={submit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div><label className="block font-semibold mb-2">Tipo *</label>
                <select className="input-field" value={form.tipo} onChange={e => setForm({...form, tipo: e.target.value})}>
                  {Object.entries(tipoLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select></div>
              <div><label className="block font-semibold mb-2">Proveedor</label>
                <input className="input-field" value={form.proveedor} onChange={e => setForm({...form, proveedor: e.target.value})} placeholder="Visa, Mastercard..." /></div>
              <div><label className="block font-semibold mb-2">Ultimos 4 digitos</label>
                <input maxLength="4" className="input-field font-mono" value={form.ultimosDigitos} onChange={e => setForm({...form, ultimosDigitos: e.target.value})} /></div>
              <div><label className="block font-semibold mb-2">Nombre del Titular</label>
                <input className="input-field" value={form.nombreTitular} onChange={e => setForm({...form, nombreTitular: e.target.value})} /></div>
              <div><label className="block font-semibold mb-2">Fecha de Expiracion</label>
                <input type="date" className="input-field" value={form.fechaExpiracion} onChange={e => setForm({...form, fechaExpiracion: e.target.value})} /></div>
              <div className="flex items-end">
                <label className="flex items-center gap-2">
                  <input type="checkbox" checked={form.predeterminado} onChange={e => setForm({...form, predeterminado: e.target.checked})} />
                  <span>Marcar como predeterminado</span>
                </label>
              </div>
            </div>
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-sm text-gray-600">
              <p className="mb-2">
                <strong>Autorización de tratamiento de datos (Habeas Data – Ley 1581 de 2012).</strong>{' '}
                Tus datos de pago se usan únicamente para gestionar tu suscripción y facturación. No almacenamos
                el número completo de la tarjeta (solo los últimos 4 dígitos). Puedes consultar, actualizar o
                solicitar la supresión de tus datos en cualquier momento.
              </p>
              <label className="flex items-start gap-2 cursor-pointer">
                <input type="checkbox" className="mt-1" checked={form.autorizacionDatos}
                  onChange={e => setForm({ ...form, autorizacionDatos: e.target.checked })} />
                <span>Autorizo de forma libre, previa, expresa e informada el tratamiento de mis datos personales
                  conforme a la <a href="/legal/politica-datos" target="_blank" rel="noopener" className="text-primary-700 underline">política de privacidad</a>.</span>
              </label>
            </div>
            <button type="submit" disabled={!form.autorizacionDatos}
              className="bg-primary-700 hover:bg-primary-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium py-2 px-4 rounded-lg">Guardar</button>
          </form>
        </div>
      )}

      <div className="space-y-3">
        {items.map(m => (
          <div key={m.id} className="card flex justify-between items-center">
            <div className="flex items-center gap-4">
              <FaCreditCard size={32} className="text-primary-700" />
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-bold">{tipoLabels[m.tipo] || m.tipo}</p>
                  {m.predeterminado && <span className="text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded">Predeterminado</span>}
                </div>
                <p className="text-sm text-gray-600">
                  {m.proveedor} {m.ultimosDigitos && `•••• ${m.ultimosDigitos}`}
                  {m.nombreTitular && ` - ${m.nombreTitular}`}
                </p>
                {m.fechaExpiracion && <p className="text-xs text-gray-500">Expira: {m.fechaExpiracion}</p>}
              </div>
            </div>
            <div className="flex gap-3 text-xl">
              <button onClick={() => setDefault(m.id)} title="Marcar predeterminado" className={m.predeterminado ? 'text-yellow-500' : 'text-gray-400 hover:text-yellow-500'}>
                {m.predeterminado ? <FaStar /> : <FaRegStar />}
              </button>
              <button onClick={() => del(m.id)} className="text-red-600 hover:text-red-800"><FaTrash /></button>
            </div>
          </div>
        ))}
        {items.length === 0 && <div className="card text-center text-gray-500">No hay metodos de pago registrados</div>}
      </div>
    </div>
  );
};
export default PaymentMethodList;
