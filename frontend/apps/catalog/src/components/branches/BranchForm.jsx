import { useState, useEffect, useMemo } from 'react';
import { Autocomplete, remoteOptions } from '@saas/ui-shell';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import sedeService from '../../services/sedeService';
import cityService from '../../services/cityService';

const BranchForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const ciudadSrc = useMemo(() => remoteOptions((q) => cityService.search(q), { sublabel: 'pais' }), []);
  const [ciudadLabel, setCiudadLabel] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ nombre: '', ciudadId: '', direccion: '', telefono: '', email: '' });
  useEffect(() => {
    if (isEdit) sedeService.getById(id).then(s => {
      setForm({
        nombre: s.nombre || '', ciudadId: s.ciudadId || '', direccion: s.direccion || '',
        telefono: s.telefono || '', email: s.email || '',
      });
      setCiudadLabel(s.ciudadNombre || '');
    }).catch(() => setError('Error al cargar'));
  }, [id]);
  const onChange = e => setForm({ ...form, [e.target.name]: e.target.value });
  const onSubmit = async e => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = { ...form, ciudadId: parseInt(form.ciudadId) };
      if (isEdit) await sedeService.update(id, payload); else await sedeService.create(payload);
      navigate('/catalog/branches');
    } catch (err) { setError(err.response?.data?.message || 'Error al guardar'); }
    finally { setLoading(false); }
  };
  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar Sede' : 'Nueva Sede'}</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={onSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2"><label className="block font-semibold mb-2">Nombre de la sede *</label>
            <input name="nombre" value={form.nombre} onChange={onChange} className="input-field" required placeholder="Ej: Sede Principal, Local Centro..." /></div>
          <div><label className="block font-semibold mb-2">Ciudad *</label>
            <Autocomplete source={ciudadSrc} value={form.ciudadId} valueLabel={ciudadLabel}
              onChange={(v, o) => { setForm((f) => ({ ...f, ciudadId: v })); setCiudadLabel(o?.label || ''); }}
              placeholder="Escribe la ciudad…" required /></div>
          <div><label className="block font-semibold mb-2">Teléfono</label>
            <input name="telefono" value={form.telefono} onChange={onChange} className="input-field" /></div>
          <div className="md:col-span-2"><label className="block font-semibold mb-2">Dirección</label>
            <input name="direccion" value={form.direccion} onChange={onChange} className="input-field" /></div>
          <div className="md:col-span-2"><label className="block font-semibold mb-2">Email</label>
            <input type="email" name="email" value={form.email} onChange={onChange} className="input-field" /></div>
          <div className="md:col-span-2 flex gap-3 pt-4 border-t">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : (isEdit ? 'Actualizar' : 'Crear')}</button>
            <button type="button" onClick={() => navigate('/catalog/branches')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default BranchForm;
