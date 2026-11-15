import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import categoryService from '../../services/categoryService';

const CategoryForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [form, setForm] = useState({ nombre: '', codigo: '', descripcion: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { if (isEdit) (async () => {
    try { const c = await categoryService.getById(id); setForm({ nombre: c.nombre, codigo: c.codigo, descripcion: c.descripcion || '' }); }
    catch { setError('Error al cargar'); }
  })(); }, [id]);

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      isEdit ? await categoryService.update(id, form) : await categoryService.create(form);
      navigate('/catalog/categories');
    } catch (err) { setError(err.response?.data?.message || 'Error'); } finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nueva'} Categoria</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={submit} className="space-y-4">
          <div><label className="block font-semibold mb-2">Nombre *</label>
            <input className="input-field" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required /></div>
          <div><label className="block font-semibold mb-2">Codigo *</label>
            <input className="input-field font-mono" value={form.codigo} onChange={e => setForm({...form, codigo: e.target.value.toUpperCase()})} required /></div>
          <div><label className="block font-semibold mb-2">Descripcion</label>
            <textarea className="input-field" rows="3" value={form.descripcion} onChange={e => setForm({...form, descripcion: e.target.value})} /></div>
          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/catalog/categories')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default CategoryForm;
