import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import clientService from '../../services/clientService';

const ClientForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [form, setForm] = useState({ nombre: '', tipo: 'minorista', documento: '', email: '', telefono: '', direccion: '', limiteCredito: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { if (isEdit) (async () => {
    try { const c = await clientService.getById(id);
      setForm({ nombre: c.nombre, tipo: c.tipo, documento: c.documento || '', email: c.email || '', telefono: c.telefono || '', direccion: c.direccion || '', limiteCredito: c.limiteCredito || '' }); }
    catch { setError('Error al cargar'); }
  })(); }, [id]);

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = { ...form, limiteCredito: form.limiteCredito ? parseFloat(form.limiteCredito) : 0 };
      isEdit ? await clientService.update(id, payload) : await clientService.create(payload);
      navigate('/catalog/clients');
    } catch (err) { setError(err.response?.data?.message || 'Error'); } finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nuevo'} Cliente</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div><label className="block font-semibold mb-2">Nombre *</label>
              <input className="input-field" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required /></div>
            <div><label className="block font-semibold mb-2">Tipo</label>
              <select className="input-field" value={form.tipo} onChange={e => setForm({...form, tipo: e.target.value})}>
                <option value="minorista">Minorista</option><option value="mayorista">Mayorista</option>
              </select></div>
            <div><label className="block font-semibold mb-2">Documento</label>
              <input className="input-field" value={form.documento} onChange={e => setForm({...form, documento: e.target.value})} /></div>
            <div><label className="block font-semibold mb-2">Limite de Credito</label>
              <input type="number" step="0.01" className="input-field" value={form.limiteCredito} onChange={e => setForm({...form, limiteCredito: e.target.value})} /></div>
            <div><label className="block font-semibold mb-2">Email</label>
              <input type="email" className="input-field" value={form.email} onChange={e => setForm({...form, email: e.target.value})} /></div>
            <div><label className="block font-semibold mb-2">Telefono</label>
              <input className="input-field" value={form.telefono} onChange={e => setForm({...form, telefono: e.target.value})} /></div>
            <div className="col-span-2"><label className="block font-semibold mb-2">Direccion</label>
              <textarea className="input-field" rows="2" value={form.direccion} onChange={e => setForm({...form, direccion: e.target.value})} /></div>
          </div>
          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/catalog/clients')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default ClientForm;
