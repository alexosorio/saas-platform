import { useState, useEffect, useMemo } from 'react';
import { Autocomplete, lazyOptions } from '@saas/ui-shell';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { FaSave, FaTimes, FaPlus } from 'react-icons/fa';
import warehouseService from '../../services/warehouseService';
import sedeService from '../../services/sedeService';

const WarehouseForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [sinSedes, setSinSedes] = useState(false);
  const [sedeLabel, setSedeLabel] = useState('');
  const sedeSrc = useMemo(() => lazyOptions(async () => {
    const l = await sedeService.getActive();
    setSinSedes(l.length === 0);
    return l;
  }, { sublabel: 'ciudadNombre' }), []);
  const [form, setForm] = useState({ sedeId: '', nombre: '', codigo: '', descripcion: '', capacidadM2: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (isEdit) (async () => {
      try { const b = await warehouseService.getById(id);
        setForm({ sedeId: b.sedeId, nombre: b.nombre, codigo: b.codigo, descripcion: b.descripcion || '', capacidadM2: b.capacidadM2 || '' });
        setSedeLabel(b.sedeNombre || ''); }
      catch { setError('Error al cargar'); }
    })();
  }, [id]);
  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = { ...form, sedeId: parseInt(form.sedeId), capacidadM2: form.capacidadM2 ? parseFloat(form.capacidadM2) : null };
      isEdit ? await warehouseService.update(id, payload) : await warehouseService.create(payload);
      navigate('/catalog/warehouses');
    } catch (err) { setError(err.response?.data?.message || 'Error'); } finally { setLoading(false); }
  };
  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nueva'} Bodega</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        {sinSedes && (
          <div className="bg-yellow-50 border border-yellow-300 text-yellow-800 px-4 py-3 rounded mb-4 flex items-center justify-between">
            <span>No tienes sedes activas. Una bodega debe pertenecer a una sede.</span>
            <Link to="/catalog/branches/new" className="btn-primary flex items-center gap-2 text-sm"><FaPlus /> Crear sede</Link>
          </div>
        )}
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div><label className="block font-semibold mb-2">Sede *</label>
              <Autocomplete source={sedeSrc} value={form.sedeId} valueLabel={sedeLabel}
                onChange={(v, o) => { setForm((f) => ({ ...f, sedeId: v })); setSedeLabel(o?.label || ''); }}
                placeholder="Buscar sede…" required /></div>
            <div><label className="block font-semibold mb-2">Codigo *</label>
              <input className="input-field font-mono" value={form.codigo} onChange={e => setForm({...form, codigo: e.target.value.toUpperCase()})} required /></div>
            <div><label className="block font-semibold mb-2">Nombre *</label>
              <input className="input-field" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required /></div>
            <div><label className="block font-semibold mb-2">Capacidad m2</label>
              <input type="number" step="0.01" className="input-field" value={form.capacidadM2} onChange={e => setForm({...form, capacidadM2: e.target.value})} /></div>
          </div>
          <div><label className="block font-semibold mb-2">Descripcion</label>
            <textarea className="input-field" rows="2" value={form.descripcion} onChange={e => setForm({...form, descripcion: e.target.value})} /></div>
          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading || sinSedes} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/catalog/warehouses')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default WarehouseForm;
