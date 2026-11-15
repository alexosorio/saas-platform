import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import characteristicService from '../../services/characteristicService';

const CharacteristicForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [form, setForm] = useState({ nombre: '', codigo: '', tipoDato: 'texto', opcionesLista: '', obligatorio: false, aplicableA: 'todos' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { if (isEdit) (async () => {
    try { const all = await characteristicService.getAll(); const c = all.find(x => x.id === parseInt(id));
      if (c) setForm({ nombre: c.nombre, codigo: c.codigo, tipoDato: c.tipoDato, opcionesLista: c.opcionesLista || '', obligatorio: c.obligatorio, aplicableA: c.aplicableA }); }
    catch { setError('Error al cargar'); }
  })(); }, [id]);

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try { isEdit ? await characteristicService.update(id, form) : await characteristicService.create(form); navigate('/catalog/characteristics'); }
    catch (err) { setError(err.response?.data?.message || 'Error'); } finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nueva'} Caracteristica</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div><label className="block font-semibold mb-2">Nombre *</label>
              <input className="input-field" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required /></div>
            <div><label className="block font-semibold mb-2">Codigo *</label>
              <input className="input-field font-mono" value={form.codigo} onChange={e => setForm({...form, codigo: e.target.value.toLowerCase()})} required /></div>
            <div><label className="block font-semibold mb-2">Tipo de Dato</label>
              <select className="input-field" value={form.tipoDato} onChange={e => setForm({...form, tipoDato: e.target.value})}>
                <option value="texto">Texto</option><option value="numero">Numero</option><option value="fecha">Fecha</option>
                <option value="booleano">Booleano</option><option value="lista">Lista</option>
              </select></div>
            <div><label className="block font-semibold mb-2">Aplica a</label>
              <select className="input-field" value={form.aplicableA} onChange={e => setForm({...form, aplicableA: e.target.value})}>
                <option value="todos">Todos</option><option value="moviles">Moviles</option><option value="accesorios">Accesorios</option>
              </select></div>
          </div>
          {form.tipoDato === 'lista' && (
            <div><label className="block font-semibold mb-2">Opciones (separadas por coma)</label>
              <input className="input-field" value={form.opcionesLista} onChange={e => setForm({...form, opcionesLista: e.target.value})} placeholder="Rojo, Azul, Negro" /></div>
          )}
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={form.obligatorio} onChange={e => setForm({...form, obligatorio: e.target.checked})} />
            <span>Obligatorio</span>
          </label>
          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/catalog/characteristics')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default CharacteristicForm;
