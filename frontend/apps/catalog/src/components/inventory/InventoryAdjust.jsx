import { useState, useMemo } from 'react';
import { Autocomplete, lazyOptions } from '@saas/ui-shell';
import { useNavigate } from 'react-router-dom';
import { FaSave, FaTimes } from 'react-icons/fa';
import warehouseService from '../../services/warehouseService';
import inventoryService from '../../services/inventoryService';

const InventoryAdjust = () => {
  const navigate = useNavigate();
  const bodegaSrc = useMemo(() => lazyOptions(() => warehouseService.getAll().then(l => l.filter(w => w.activo !== false)), {
    label: (w) => `${w.nombre} (${w.codigo})`, sublabel: 'sedeNombre',
  }), []);
  const [form, setForm] = useState({ bodegaId: '', unidadId: '', cantidad: 1, tipo: 'entrada', motivo: '' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault(); setError(''); setOk(''); setLoading(true);
    try {
      await inventoryService.adjust({
        bodegaId: parseInt(form.bodegaId), unidadId: parseInt(form.unidadId),
        cantidad: parseInt(form.cantidad), tipo: form.tipo, motivo: form.motivo || null,
      });
      setOk('Ajuste registrado correctamente');
      setForm({ ...form, unidadId: '', cantidad: 1, motivo: '' });
    } catch (err) { setError(err.response?.data?.message || 'Error al ajustar'); }
    finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Ajustar Inventario</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        {ok && <div className="bg-green-100 text-green-700 px-4 py-3 rounded mb-4">{ok}</div>}
        <form onSubmit={submit} className="space-y-4">
          <div><label className="block font-semibold mb-2">Bodega *</label>
            <Autocomplete source={bodegaSrc} value={form.bodegaId}
              onChange={(v) => setForm((f) => ({ ...f, bodegaId: v }))}
              placeholder="Buscar bodega…" required /></div>
          <div className="grid grid-cols-2 gap-4">
            <div><label className="block font-semibold mb-2">Unidad ID *</label>
              <input type="number" className="input-field" value={form.unidadId} onChange={e => setForm({...form, unidadId: e.target.value})} required /></div>
            <div><label className="block font-semibold mb-2">Cantidad *</label>
              <input type="number" min="1" className="input-field" value={form.cantidad} onChange={e => setForm({...form, cantidad: e.target.value})} required /></div>
          </div>
          <div><label className="block font-semibold mb-2">Tipo de Movimiento</label>
            <select className="input-field" value={form.tipo} onChange={e => setForm({...form, tipo: e.target.value})}>
              <option value="entrada">Entrada (sumar)</option>
              <option value="salida">Salida (restar)</option>
              <option value="ajuste">Ajuste (fijar cantidad)</option>
            </select></div>
          <div><label className="block font-semibold mb-2">Motivo</label>
            <input className="input-field" value={form.motivo} onChange={e => setForm({...form, motivo: e.target.value})} placeholder="Ej: Compra a proveedor" /></div>
          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Procesando...' : 'Registrar'}</button>
            <button type="button" onClick={() => navigate('/catalog/inventory')} className="btn-secondary flex items-center gap-2"><FaTimes /> Volver</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default InventoryAdjust;
