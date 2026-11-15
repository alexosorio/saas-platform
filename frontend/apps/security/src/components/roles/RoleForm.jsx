import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import roleService from '../../services/roleService';

const RoleForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [form, setForm] = useState({ nombre: '', descripcion: '' });
  const [esSistema, setEsSistema] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { if (isEdit) (async () => {
    try { const r = await roleService.getRoleById(id); setForm({ nombre: r.nombre, descripcion: r.descripcion || '' }); setEsSistema(r.esSistema); }
    catch { setError('Error al cargar el rol'); }
  })(); }, [id]);

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      if (isEdit) { await roleService.updateRole(id, form); navigate('/roles'); }
      else {
        const nuevo = await roleService.createRole(form);
        navigate(nuevo?.id ? `/roles/${nuevo.id}/permisos` : '/roles');   // asignar módulos de inmediato
      }
    } catch (err) { setError(err.response?.data?.message || 'Error al guardar'); }
    finally { setLoading(false); }
  };

  return (
    <div className="max-w-xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nuevo'} Rol</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        {esSistema && <div className="bg-amber-50 text-amber-800 px-4 py-2 rounded mb-4 text-sm">Este es un rol del sistema; edítalo con cuidado.</div>}
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block font-semibold mb-2">Nombre *</label>
            <input className="input-field" value={form.nombre} required
              onChange={e => setForm({ ...form, nombre: e.target.value })} />
          </div>
          <div>
            <label className="block font-semibold mb-2">Descripción</label>
            <textarea className="input-field" rows={3} value={form.descripcion}
              onChange={e => setForm({ ...form, descripcion: e.target.value })} />
          </div>
          <p className="text-xs text-gray-400">
            {isEdit
              ? <>Los módulos y permisos de este rol se gestionan en <Link to={`/roles/${id}/permisos`} className="underline text-primary-700">Módulos y permisos</Link>.</>
              : 'Al guardar podrás asignarle los módulos y sus permisos.'}
          </p>
          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/roles')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default RoleForm;
