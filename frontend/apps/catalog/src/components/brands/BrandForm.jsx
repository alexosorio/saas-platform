import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes, FaUpload, FaTrash } from 'react-icons/fa';
import brandService from '../../services/brandService';
import uploadService from '../../services/uploadService';

const CATALOG_API = import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8083';
const src = (url) => (url ? (url.startsWith('http') ? url : `${CATALOG_API}${url}`) : '');

const BrandForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [form, setForm] = useState({ nombre: '', codigo: '', logoUrl: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => { if (isEdit) (async () => {
    try { const b = await brandService.getById(id); setForm({ nombre: b.nombre, codigo: b.codigo, logoUrl: b.logoUrl || '' }); }
    catch { setError('Error al cargar'); }
  })(); }, [id]);

  const onFile = async (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    const okTypes = ['image/svg+xml', 'image/png'];
    if (!okTypes.includes(f.type)) { setError('El logo debe ser SVG o PNG.'); return; }
    if (f.size > 5 * 1024 * 1024) { setError('El archivo supera 5MB.'); return; }
    setError(''); setUploading(true);
    try {
      const r = await uploadService.uploadImage(f);
      setForm((prev) => ({ ...prev, logoUrl: r.url }));
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo subir el logo');
    } finally { setUploading(false); }
  };

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try { isEdit ? await brandService.update(id, form) : await brandService.create(form); navigate('/catalog/brands'); }
    catch (err) { setError(err.response?.data?.message || 'Error'); } finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nueva'} Marca</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={submit} className="space-y-4">
          <div><label className="block font-semibold mb-2">Nombre *</label>
            <input className="input-field" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required /></div>
          <div><label className="block font-semibold mb-2">Codigo *</label>
            <input className="input-field font-mono" value={form.codigo} onChange={e => setForm({...form, codigo: e.target.value.toUpperCase()})} required /></div>

          <div>
            <label className="block font-semibold mb-2">Logo (SVG o PNG)</label>
            <div className="flex items-center gap-4">
              <div className="w-24 h-24 rounded-lg border border-dashed border-gray-300 flex items-center justify-center bg-gray-50 overflow-hidden">
                {form.logoUrl
                  ? <img src={src(form.logoUrl)} alt="logo" className="max-w-full max-h-full object-contain" />
                  : <span className="text-xs text-gray-400 text-center px-2">Sin logo</span>}
              </div>
              <div className="flex flex-col gap-2">
                <label className="btn-secondary flex items-center gap-2 cursor-pointer w-fit">
                  <FaUpload /> {uploading ? 'Subiendo...' : (form.logoUrl ? 'Cambiar logo' : 'Subir logo')}
                  <input type="file" accept="image/svg+xml,image/png,.svg,.png" className="hidden" onChange={onFile} disabled={uploading} />
                </label>
                {form.logoUrl && (
                  <button type="button" onClick={() => setForm({ ...form, logoUrl: '' })}
                    className="text-red-600 text-sm flex items-center gap-1"><FaTrash /> Quitar</button>
                )}
                <p className="text-xs text-gray-400">Formatos: SVG o PNG. Máx 5MB.</p>
              </div>
            </div>
          </div>

          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading || uploading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/catalog/brands')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default BrandForm;
