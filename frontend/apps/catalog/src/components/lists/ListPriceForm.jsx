import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes, FaCalculator } from 'react-icons/fa';
import listService from '../../services/listService';

const ListPriceForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  const [form, setForm] = useState({
    nombre: '', tipoCliente: 'minorista', descripcion: '',
    baseCalculo: 'precio_venta', tipoAjuste: 'margen_porcentaje', valorAjuste: '0',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { if (isEdit) (async () => {
    try {
      const l = await listService.getById(id);
      setForm({
        nombre: l.nombre, tipoCliente: l.tipoCliente, descripcion: l.descripcion || '',
        baseCalculo: l.baseCalculo || 'precio_venta',
        tipoAjuste: l.tipoAjuste || 'margen_porcentaje',
        valorAjuste: l.valorAjuste != null ? String(l.valorAjuste) : '0',
      });
    } catch { setError('Error al cargar'); }
  })(); }, [id]);

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = { ...form, valorAjuste: parseFloat(form.valorAjuste) || 0 };
      isEdit ? await listService.update(id, payload) : await listService.create(payload);
      navigate('/catalog/lists');
    } catch (err) { setError(err.response?.data?.message || 'Error'); }
    finally { setLoading(false); }
  };

  // Texto explicativo de la formula
  const baseLabel = form.baseCalculo === 'precio_venta' ? 'precio de venta' : 'precio de compra';
  const formulaTexto = form.tipoAjuste === 'valor_fijo'
    ? `precio = ${baseLabel} + $${form.valorAjuste || 0}  (ej: 100 + ${form.valorAjuste || 0} = ${100 + (parseFloat(form.valorAjuste)||0)})`
    : `precio = ${baseLabel} × (1 ${parseFloat(form.valorAjuste) < 0 ? '-' : '+'} ${Math.abs(parseFloat(form.valorAjuste) || 0)}/100)`;

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar' : 'Nueva'} Lista de Precios</h1>
      <div className="card">
        {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
        <form onSubmit={submit} className="space-y-4">
          <div><label className="block font-semibold mb-2">Nombre *</label>
            <input className="input-field" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required /></div>

          <div><label className="block font-semibold mb-2">Audiencia *</label>
            <select className="input-field" value={form.tipoCliente} onChange={e => setForm({...form, tipoCliente: e.target.value})}>
              <option value="minorista">Minorista (cliente final)</option>
              <option value="mayorista">Mayorista</option>
            </select></div>

          {/* Configuracion de la formula */}
          <div className="bg-blue-50 border border-blue-200 rounded p-4 space-y-4">
            <h3 className="font-bold flex items-center gap-2 text-primary-700"><FaCalculator /> Fórmula de precios</h3>
            <p className="text-sm text-gray-600">
              La lista expone automáticamente todos los productos activos. El precio de cada uno se calcula con esta regla:
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block font-semibold mb-2">Calcular sobre</label>
                <select className="input-field" value={form.baseCalculo}
                  onChange={e => setForm({...form, baseCalculo: e.target.value})}>
                  <option value="precio_venta">Precio de venta del producto</option>
                  <option value="precio_compra">Precio de compra del producto</option>
                </select>
              </div>
              <div>
                <label className="block font-semibold mb-2">Tipo de ajuste</label>
                <select className="input-field" value={form.tipoAjuste}
                  onChange={e => setForm({...form, tipoAjuste: e.target.value})}>
                  <option value="margen_porcentaje">Margen porcentual (%)</option>
                  <option value="valor_fijo">Valor fijo (se suma a la base)</option>
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block font-semibold mb-2">
                  {form.tipoAjuste === 'margen_porcentaje' ? 'Porcentaje (negativo = descuento)' : 'Valor fijo'}
                </label>
                <input type="number" step="0.01" className="input-field font-mono"
                  value={form.valorAjuste} onChange={e => setForm({...form, valorAjuste: e.target.value})}
                  placeholder={form.tipoAjuste === 'margen_porcentaje' ? 'Ej: 30 o -15' : 'Ej: 50000'} />
              </div>
            </div>

            <div className="bg-white border rounded p-3 text-sm">
              <span className="text-gray-500">Resultado:</span> <span className="font-mono">{formulaTexto}</span>
            </div>

            <div className="text-xs text-gray-500 space-y-1">
              <p><strong>Ejemplos típicos:</strong></p>
              <p>• Cliente final = sobre <em>precio de venta</em>, margen <em>0%</em> (usa el precio de venta tal cual).</p>
              <p>• Mayorista = sobre <em>precio de venta</em>, margen <em>-15%</em> (15% de descuento).</p>
              <p>• Lista a costo+40% = sobre <em>precio de compra</em>, margen <em>40%</em>.</p>
            </div>
          </div>

          <div><label className="block font-semibold mb-2">Descripcion</label>
            <textarea className="input-field" rows="2" value={form.descripcion} onChange={e => setForm({...form, descripcion: e.target.value})} /></div>

          <div className="flex gap-3 pt-4 border-t">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2"><FaSave /> {loading ? 'Guardando...' : 'Guardar'}</button>
            <button type="button" onClick={() => navigate('/catalog/lists')} className="btn-secondary flex items-center gap-2"><FaTimes /> Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default ListPriceForm;
