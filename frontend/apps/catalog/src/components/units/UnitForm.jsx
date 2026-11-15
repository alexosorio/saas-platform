import { useState, useEffect, useMemo } from 'react';
import { Autocomplete, lazyOptions, remoteOptions } from '@saas/ui-shell';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes, FaBarcode } from 'react-icons/fa';
import unitService from '../../services/unitService';
import productService from '../../services/productService';
import characteristicService from '../../services/characteristicService';
import warehouseService from '../../services/warehouseService';
import supplierService from '../../services/supplierService';

const UnitForm = () => {
  const { id: productoId } = useParams();   // ruta /products/:id/units/new
  const navigate = useNavigate();

  const [product, setProduct] = useState(null);
  const [characteristics, setCharacteristics] = useState([]);
  // Bajo demanda: proveedores se buscan en el servidor; bodegas se cargan al abrir el campo
  const proveedorSrc = useMemo(() => remoteOptions((q) => supplierService.search(q), { sublabel: 'email' }), []);
  const bodegaSrc = useMemo(() => lazyOptions(() => warehouseService.getAll().then(l => l.filter(w => w.activo !== false)), {
    label: (w) => `${w.nombre} (${w.codigo})`, sublabel: 'sedeNombre',
  }), []);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Si el producto NO requiere IMEI (cargadores, cables, accesorios),
  // se oculta esa seccion del formulario. Empieza en true por compatibilidad.
  const [requiereImei, setRequiereImei] = useState(true);

  const [form, setForm] = useState({
    codigoUnico: '', codigoBarras: '', imei1: '', imei2: '', condicion: 'nuevo',
    nivelBateria: '', precioCompra: '', precioVenta: '', notas: '', bodegaId: '', proveedorId: '',
  });
  const [caracteristicas, setCaracteristicas] = useState({});

  useEffect(() => { load(); }, [productoId]);

  const load = async () => {
    try {
      const [p, chars] = await Promise.all([
        productService.getById(productoId),
        characteristicService.getAll(),
      ]);
      setProduct(p); setCharacteristics(chars);
    } catch (err) { console.error(err); }
  };

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const onCharChange = (codigo, valor) => setCaracteristicas({ ...caracteristicas, [codigo]: valor });

  const onSubmit = async (e) => {
    e.preventDefault();
    if (requiereImei && (errImei1 || errImei2)) { setError('Corrige el IMEI antes de guardar'); return; }
    setError(''); setLoading(true);
    try {
      const limpio = {};
      Object.entries(caracteristicas).forEach(([k, v]) => { if (v !== '' && v != null) limpio[k] = String(v); });
      await unitService.create({
        productoId: parseInt(productoId),
        codigoUnico: form.codigoUnico,
        codigoBarras: form.codigoBarras.trim() || null,
        imei1: requiereImei ? (form.imei1 || null) : null,
        imei2: requiereImei ? (form.imei2 || null) : null,
        condicion: form.condicion,
        nivelBateria: requiereImei && form.nivelBateria ? parseInt(form.nivelBateria) : null,
        precioCompra: parseFloat(form.precioCompra),
        precioVenta: form.precioVenta !== '' ? parseFloat(form.precioVenta) : null,
        notas: form.notas || null,
        bodegaId: form.bodegaId ? parseInt(form.bodegaId) : null,
        proveedorId: form.proveedorId ? parseInt(form.proveedorId) : null,
        caracteristicas: limpio,
      });
      navigate(`/catalog/products/${productoId}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al crear la unidad');
    } finally { setLoading(false); }
  };

  const margen = (form.precioVenta !== '' && parseFloat(form.precioCompra) > 0)
    ? (((parseFloat(form.precioVenta) - parseFloat(form.precioCompra)) / parseFloat(form.precioCompra)) * 100).toFixed(1)
    : null;

  // Solo las características que aplican al tipo de producto; el color primero
  const aplicables = characteristics
    .filter(c => !c.aplicableA || c.aplicableA === 'todos' || c.aplicableA === (requiereImei ? 'moviles' : 'accesorios'))
    .sort((a, b) => (a.codigo === 'color' ? -1 : b.codigo === 'color' ? 1 : 0));

  // IMEI: 15 dígitos y dígito de control (Luhn); el servidor valida igual
  const imeiError = (v) => {
    if (!v) return null;
    const n = v.replace(/[\s-]/g, '');
    if (!/^\d+$/.test(n)) return 'Solo números';
    if (n.length !== 15) return `Debe tener 15 dígitos (tiene ${n.length})`;
    let suma = 0;
    for (let i = 0; i < 15; i++) { let d = +n[14 - i]; if (i % 2) { d *= 2; if (d > 9) d -= 9; } suma += d; }
    return suma % 10 ? 'Dígito de verificación inválido: revisa el número' : null;
  };
  const errImei1 = imeiError(form.imei1), errImei2 = imeiError(form.imei2);

  const renderCharInput = (c) => {
    const val = caracteristicas[c.codigo] ?? '';
    if (c.tipoDato === 'booleano') {
      return (
        <select className="input-field" value={val} required={c.obligatorio} onChange={(e) => onCharChange(c.codigo, e.target.value)}>
          <option value="">--</option><option value="true">Si</option><option value="false">No</option>
        </select>
      );
    }
    if (c.tipoDato === 'lista' && c.opcionesLista) {
      const opts = c.opcionesLista.split(',').map(o => o.trim());
      return (
        <select className="input-field" value={val} required={c.obligatorio} onChange={(e) => onCharChange(c.codigo, e.target.value)}>
          <option value="">--</option>
          {opts.map(o => <option key={o} value={o}>{o}</option>)}
        </select>
      );
    }
    const type = c.tipoDato === 'numero' ? 'number' : c.tipoDato === 'fecha' ? 'date' : 'text';
    if (c.codigo === 'color') {
      // Sugiere los colores que ya tiene el producto para escribirlos siempre igual
      return (<>
        <input type="text" className="input-field" list="colores-producto" value={val} required={c.obligatorio}
          placeholder="Ej: Negro, Azul titanio…" onChange={(e) => onCharChange(c.codigo, e.target.value)} />
        <datalist id="colores-producto">
          {(product?.coloresDisponibles || []).map(col => <option key={col} value={col} />)}
        </datalist>
      </>);
    }
    return <input type={type} className="input-field" value={val} required={c.obligatorio} onChange={(e) => onCharChange(c.codigo, e.target.value)} />;
  };

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold mb-2">Nueva Unidad</h1>
      {product && <p className="text-gray-500 mb-6">Producto: <strong>{product.nombre}</strong></p>}

      <div className="card">
        {error && <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">{error}</div>}
        <form onSubmit={onSubmit} className="space-y-4">

          {/* Toggle: con/sin IMEI segun el tipo de producto */}
          <div className="bg-gray-50 p-3 rounded flex gap-4 items-center">
            <span className="font-semibold text-gray-700">Tipo de unidad:</span>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="radio" name="tipoUnidad" checked={requiereImei}
                onChange={() => setRequiereImei(true)} />
              <span>Con IMEI (movil / dispositivo)</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="radio" name="tipoUnidad" checked={!requiereImei}
                onChange={() => setRequiereImei(false)} />
              <span>Sin IMEI (cargador / cable / accesorio)</span>
            </label>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Codigo Unico *</label>
              <input type="text" name="codigoUnico" value={form.codigoUnico} onChange={onChange} className="input-field font-mono" required />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2"><FaBarcode className="text-gray-500" /> Código de barras</label>
              <input type="text" name="codigoBarras" value={form.codigoBarras} maxLength={64}
                onChange={onChange} onKeyDown={(e) => { if (e.key === 'Enter') e.preventDefault(); }}
                className="input-field font-mono" placeholder="Escanéalo del empaque de esta unidad (opcional)" />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Precio de compra *</label>
              <input type="number" step="0.01" min="0" name="precioCompra" value={form.precioCompra} onChange={onChange} className="input-field" required />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2">
                Precio de venta {margen !== null && <span className="text-green-600 text-xs font-normal">(margen {margen}%)</span>}
              </label>
              <input type="number" step="0.01" min="0" name="precioVenta" value={form.precioVenta} onChange={onChange} className="input-field" />
            </div>
            {requiereImei && (<>
              <div>
                <label className="block text-gray-700 font-semibold mb-2">IMEI 1</label>
                <input type="text" name="imei1" value={form.imei1} onChange={onChange} inputMode="numeric" maxLength={17} className={`input-field font-mono ${errImei1 ? 'border-red-400' : ''}`} />
                {errImei1 && <p className="text-xs text-red-600 mt-1">{errImei1}</p>}
              </div>
              <div>
                <label className="block text-gray-700 font-semibold mb-2">IMEI 2</label>
                <input type="text" name="imei2" value={form.imei2} onChange={onChange} inputMode="numeric" maxLength={17} className={`input-field font-mono ${errImei2 ? 'border-red-400' : ''}`} />
                {errImei2 && <p className="text-xs text-red-600 mt-1">{errImei2}</p>}
              </div>
            </>)}
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Condicion</label>
              <select name="condicion" value={form.condicion} onChange={onChange} className="input-field">
                <option value="nuevo">Nuevo</option>
                <option value="usado">Usado</option>
              </select>
            </div>
            {requiereImei && (
              <div>
                <label className="block text-gray-700 font-semibold mb-2">Nivel de Bateria (%)</label>
                <input type="number" min="0" max="100" name="nivelBateria" value={form.nivelBateria} onChange={onChange} className="input-field" />
              </div>
            )}
            <div className="md:col-span-2">
              <label className="block text-gray-700 font-semibold mb-2">Proveedor de esta unidad</label>
              <Autocomplete source={proveedorSrc} value={form.proveedorId}
                onChange={(v) => setForm((f) => ({ ...f, proveedorId: v }))}
                placeholder="Escribe el proveedor (opcional)…" />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Bodega (ingreso a inventario)</label>
              <Autocomplete source={bodegaSrc} value={form.bodegaId}
                onChange={(v) => setForm((f) => ({ ...f, bodegaId: v }))}
                placeholder="Sin bodega = no ingresa a inventario aún" />
            </div>
          </div>

          {aplicables.length > 0 && (
            <div className="border-t pt-4">
              <h3 className="font-semibold text-gray-700 mb-3">Características de la unidad</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {aplicables.map(c => (
                  <div key={c.id}>
                    <label className="block text-gray-600 mb-1 text-sm">
                      {c.nombre} {c.obligatorio && <span className="text-red-500">*</span>}
                      <span className="text-gray-400 text-xs"> ({c.tipoDato})</span>
                    </label>
                    {renderCharInput(c)}
                  </div>
                ))}
              </div>
            </div>
          )}

          <div>
            <label className="block text-gray-700 font-semibold mb-2">Notas</label>
            <textarea name="notas" value={form.notas} onChange={onChange} rows="2" className="input-field" />
          </div>

          <div className="flex gap-3 pt-4 border-t">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2">
              <FaSave /> {loading ? 'Guardando...' : 'Crear Unidad'}
            </button>
            <button type="button" onClick={() => navigate(`/catalog/products/${productoId}`)} className="btn-secondary flex items-center gap-2">
              <FaTimes /> Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UnitForm;
