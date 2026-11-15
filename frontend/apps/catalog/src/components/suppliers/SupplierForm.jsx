import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import supplierService from '../../services/supplierService';

const SupplierForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    nombre: '', identificacion: '', telefono: '', email: '', direccion: '', contacto: '', notas: '',
  });

  useEffect(() => {
    if (isEdit) supplierService.getById(id).then(s => setForm({
      nombre: s.nombre || '', identificacion: s.identificacion || '', telefono: s.telefono || '',
      email: s.email || '', direccion: s.direccion || '', contacto: s.contacto || '', notas: s.notas || '',
    })).catch(() => setError('Error al cargar'));
  }, [id]);

  const onChange = e => setForm({ ...form, [e.target.name]: e.target.value });
  const onSubmit = async e => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      if (isEdit) await supplierService.update(id, form);
      else await supplierService.create(form);
      navigate('/catalog/suppliers');
    } catch (err) { setError(err.response?.data?.message || 'Error al guardar'); }
    finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar Proveedor' : 'Nuevo Proveedor'}</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={onSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="block font-semibold mb-2">Nombre *</label>
            <input name="nombre" value={form.nombre} onChange={onChange} className="input-field" required />
          </div>
          <div>
            <label className="block font-semibold mb-2">Identificación (NIT/RUT)</label>
            <input name="identificacion" value={form.identificacion} onChange={onChange} className="input-field" />
          </div>
          <div>
            <label className="block font-semibold mb-2">Persona de contacto</label>
            <input name="contacto" value={form.contacto} onChange={onChange} className="input-field" />
          </div>
          <div>
            <label className="block font-semibold mb-2">Teléfono</label>
            <input name="telefono" value={form.telefono} onChange={onChange} className="input-field" />
          </div>
          <div>
            <label className="block font-semibold mb-2">Email</label>
            <input type="email" name="email" value={form.email} onChange={onChange} className="input-field" />
          </div>
          <div className="md:col-span-2">
            <label className="block font-semibold mb-2">Dirección</label>
            <input name="direccion" value={form.direccion} onChange={onChange} className="input-field" />
          </div>
          <div className="md:col-span-2">
            <label className="block font-semibold mb-2">Notas</label>
            <textarea name="notas" value={form.notas} onChange={onChange} rows="3" className="input-field" />
          </div>
          <div className="md:col-span-2 flex gap-3 pt-4 border-t">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2">
              <FaSave /> {loading ? 'Guardando...' : (isEdit ? 'Actualizar' : 'Crear')}
            </button>
            <button type="button" onClick={() => navigate('/catalog/suppliers')} className="btn-secondary flex items-center gap-2">
              <FaTimes /> Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default SupplierForm;
