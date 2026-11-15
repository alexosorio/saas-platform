import { useState, useEffect, useMemo } from 'react';
import { Autocomplete, lazyOptions } from '@saas/ui-shell';
import { useNavigate, useParams } from 'react-router-dom';
import { FaSave, FaTimes, FaUpload, FaImage, FaMobileAlt, FaPlug } from 'react-icons/fa';
import productService from '../../services/productService';
import categoryService from '../../services/categoryService';
import brandService from '../../services/brandService';
import uploadService from '../../services/uploadService';

const CATALOG_API = import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8083';

const ProductForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(isEdit);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  // Catálogos bajo demanda: se descargan solo al interactuar con el campo
  const categoriaSrc = useMemo(() => lazyOptions(() => categoryService.getActive()), []);
  const marcaSrc = useMemo(() => lazyOptions(() => brandService.getActive()), []);
  const [labels, setLabels] = useState({ categoria: '', marca: '' });
  const [form, setForm] = useState({
    nombre: '', sku: '', descripcion: '', imagenUrl: '',
    categoriaId: '', marcaId: '',
    tipoProducto: 'accesorio',
  });

  useEffect(() => {
    if (isEdit) loadProduct();
  }, [id]);

  const loadProduct = async () => {
    try {
      setLoadingData(true);
      const p = await productService.getById(id);
      setForm({
        nombre: p.nombre || '', sku: p.sku || '', descripcion: p.descripcion || '',
        imagenUrl: p.imagenUrl || '', categoriaId: p.categoriaId || '', marcaId: p.marcaId || '',
        tipoProducto: p.tipoProducto || 'accesorio',
      });
      setLabels({ categoria: p.categoriaNombre || '', marca: p.marcaNombre || '' });
    } catch (err) { setError('Error al cargar producto'); }
    finally { setLoadingData(false); }
  };

  const setCampo = (campo, valor) => { setForm((f) => ({ ...f, [campo]: valor })); setError(''); };
  const onChange = (e) => { setForm({ ...form, [e.target.name]: e.target.value }); setError(''); };

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const payload = {
        nombre: form.nombre,
        sku: form.sku || null,
        descripcion: form.descripcion || null,
        imagenUrl: form.imagenUrl || null,
        categoriaId: parseInt(form.categoriaId),
        marcaId: form.marcaId ? parseInt(form.marcaId) : null,
        tipoProducto: form.tipoProducto,
        requiereImei: form.tipoProducto === 'movil',
      };
      if (isEdit) await productService.update(id, payload);
      else await productService.create(payload);
      navigate('/catalog/products');
    } catch (err) {
      setError(err.response?.data?.message || `Error al ${isEdit ? 'actualizar' : 'crear'} el producto`);
    } finally { setLoading(false); }
  };

  if (loadingData) return <div className="text-center py-10 text-gray-500">Cargando...</div>;

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">{isEdit ? 'Editar Producto' : 'Nuevo Producto'}</h1>
      <div className="card">
        {error && <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">{error}</div>}
        <form onSubmit={onSubmit} className="space-y-4">

          {/* Tipo de producto */}
          <div className="bg-gray-50 p-3 rounded flex gap-4 items-center flex-wrap">
            <span className="font-semibold text-gray-700">Tipo de producto:</span>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="radio" name="tipoProducto" value="movil"
                checked={form.tipoProducto === 'movil'} onChange={onChange} />
              <FaMobileAlt /> Móvil (con IMEI)
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="radio" name="tipoProducto" value="accesorio"
                checked={form.tipoProducto === 'accesorio'} onChange={onChange} />
              <FaPlug /> Accesorio (sin IMEI)
            </label>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Nombre *</label>
              <input type="text" name="nombre" value={form.nombre} onChange={onChange} className="input-field" required />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2">SKU</label>
              <input type="text" name="sku" value={form.sku} onChange={onChange} className="input-field font-mono" placeholder="Opcional" />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Categoria *</label>
              <Autocomplete source={categoriaSrc} value={form.categoriaId} valueLabel={labels.categoria}
                onChange={(v, o) => { setCampo('categoriaId', v); setLabels((l) => ({ ...l, categoria: o?.label || '' })); }}
                placeholder="Buscar categoría…" required />
            </div>
            <div>
              <label className="block text-gray-700 font-semibold mb-2">Marca</label>
              <Autocomplete source={marcaSrc} value={form.marcaId} valueLabel={labels.marca}
                onChange={(v, o) => { setCampo('marcaId', v); setLabels((l) => ({ ...l, marca: o?.label || '' })); }}
                placeholder="Buscar marca (opcional)…" />
            </div>
            <div></div>

            {/* Imagen */}
            <div className="md:col-span-2">
              <label className="block text-gray-700 font-semibold mb-2 flex items-center gap-2"><FaImage /> Imagen del producto</label>
              <div className="flex gap-3 items-start">
                <div className="w-32 h-32 border-2 border-dashed border-gray-300 rounded flex items-center justify-center bg-gray-50 overflow-hidden flex-shrink-0">
                  {form.imagenUrl ? (
                    <img src={form.imagenUrl.startsWith('http') ? form.imagenUrl : `${CATALOG_API}${form.imagenUrl}`}
                         alt="preview" className="w-full h-full object-cover" />
                  ) : <span className="text-gray-400 text-xs text-center px-2">Sin imagen</span>}
                </div>
                <div className="flex-1 space-y-2">
                  <label className="btn-secondary inline-flex items-center gap-2 cursor-pointer">
                    <FaUpload /> {uploading ? 'Subiendo...' : 'Subir imagen'}
                    <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" className="hidden"
                      onChange={async (e) => {
                        const f = e.target.files?.[0]; if (!f) return;
                        setUploading(true); setError('');
                        try { const r = await uploadService.uploadImage(f); setForm({ ...form, imagenUrl: r.url }); }
                        catch (err) { setError(err.response?.data?.message || 'Error al subir'); }
                        finally { setUploading(false); e.target.value = ''; }
                      }} />
                  </label>
                  <p className="text-xs text-gray-500">JPG, PNG, WEBP o GIF. Máximo 5MB.</p>
                  <input type="text" name="imagenUrl" value={form.imagenUrl} onChange={onChange}
                         className="input-field text-sm" placeholder="Ruta subida (/uploads/...) o URL externa (https://...)" />
                  {form.imagenUrl && (
                    <button type="button" onClick={() => setForm({...form, imagenUrl: ''})}
                            className="text-xs text-red-600 hover:underline">Quitar imagen</button>
                  )}
                </div>
              </div>
            </div>

            <div className="md:col-span-2">
              <label className="block text-gray-700 font-semibold mb-2">Descripcion</label>
              <textarea name="descripcion" value={form.descripcion} onChange={onChange} rows="4" className="input-field" />
            </div>
          </div>

          <p className="text-sm text-gray-500 bg-blue-50 p-3 rounded">
            El producto es la ficha general. El <strong>precio de compra, el precio de venta y el color</strong> se registran
            en cada <strong>unidad</strong> al ingresarla, porque pueden variar entre unidades del mismo producto. Las
            <strong> Listas de Precios</strong> aplican su fórmula sobre el precio de cada unidad.
          </p>

          <div className="flex gap-3 pt-4 border-t">
            <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2">
              <FaSave /> {loading ? 'Guardando...' : (isEdit ? 'Actualizar' : 'Crear')}
            </button>
            <button type="button" onClick={() => navigate('/catalog/products')} className="btn-secondary flex items-center gap-2">
              <FaTimes /> Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProductForm;
