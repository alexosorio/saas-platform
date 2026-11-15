import { useState, useEffect } from 'react';
import { FaMagic, FaInfoCircle } from 'react-icons/fa';
import listService from '../../services/listService';
import categoryService from '../../services/categoryService';
import brandService from '../../services/brandService';

const ESTRATEGIAS = [
  {
    value: 'MARGEN_SOBRE_COMPRA',
    label: 'Margen sobre precio de COMPRA',
    desc: 'precio = precio_compra × (1 + margen%/100). Usa el costo más reciente de las unidades del producto.',
    placeholder: 'Ej: 30 (= 30% de margen sobre el costo)',
    suffix: '%',
  },
  {
    value: 'AJUSTE_SOBRE_LISTA',
    label: 'Ajuste sobre otra lista existente',
    desc: 'precio = precio_lista_origen × (1 + ajuste%/100). Valor NEGATIVO para descuento (ej. -15%), positivo para incremento.',
    placeholder: 'Ej: -15 (= 15% de descuento) o 10 (= 10% más caro)',
    suffix: '%',
  },
  {
    value: 'VALOR_FIJO',
    label: 'Valor fijo (mismo precio para todos)',
    desc: 'Asigna el mismo precio a todos los productos seleccionados. Útil para promociones puntuales o catálogos planos.',
    placeholder: 'Ej: 50000',
    suffix: '',
  },
];

const BulkPricing = ({ lista, otrasListas, onCreated }) => {
  const [open, setOpen] = useState(false);
  const [estrategia, setEstrategia] = useState('MARGEN_SOBRE_COMPRA');
  const [valor, setValor] = useState('');
  const [listaOrigenId, setListaOrigenId] = useState('');
  const [categorias, setCategorias] = useState([]);
  const [marcas, setMarcas] = useState([]);
  const [categoriaIds, setCategoriaIds] = useState([]);
  const [marcaIds, setMarcaIds] = useState([]);
  const [sobrescribir, setSobrescribir] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (open && categorias.length === 0) {
      categoryService.getActive().then(setCategorias).catch(() => {});
      brandService.getActive().then(setMarcas).catch(() => {});
    }
  }, [open]);

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setResult(null); setSubmitting(true);
    try {
      const r = await listService.bulkPricing(lista.id, {
        estrategia,
        valor: parseFloat(valor),
        listaOrigenId: estrategia === 'AJUSTE_SOBRE_LISTA' ? parseInt(listaOrigenId) : null,
        categoriaIds: categoriaIds.length ? categoriaIds.map(Number) : null,
        marcaIds: marcaIds.length ? marcaIds.map(Number) : null,
        sobrescribir,
      });
      setResult(r);
      if (onCreated) onCreated();
    } catch (err) {
      setError(err.response?.data?.message || 'Error en la generacion');
    } finally { setSubmitting(false); }
  };

  const seleccion = ESTRATEGIAS.find(e => e.value === estrategia);

  return (
    <div className="card mt-6 bg-purple-50 border border-purple-200">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-bold flex items-center gap-2">
          <FaMagic className="text-purple-700" /> Generar precios en masa
        </h2>
        <button onClick={() => setOpen(!open)}
          className="text-purple-700 hover:text-purple-900 text-sm font-semibold">
          {open ? 'Ocultar' : 'Mostrar opciones'}
        </button>
      </div>

      {!open && (
        <p className="text-sm text-gray-600 mt-1">
          Crea precios para todos los productos (o un subconjunto) usando una regla — margen sobre el costo, ajuste sobre otra lista, o valor fijo.
        </p>
      )}

      {open && (
        <form onSubmit={submit} className="mt-4 space-y-4">
          {/* Estrategia */}
          <div>
            <label className="block font-semibold mb-2">Estrategia *</label>
            <div className="space-y-2">
              {ESTRATEGIAS.map(e => (
                <label key={e.value} className={`block border rounded p-3 cursor-pointer transition ${estrategia === e.value ? 'border-purple-500 bg-purple-100' : 'border-gray-200 bg-white hover:border-purple-300'}`}>
                  <div className="flex items-start gap-2">
                    <input type="radio" name="estrategia" value={e.value}
                      checked={estrategia === e.value}
                      onChange={() => setEstrategia(e.value)}
                      className="mt-1" />
                    <div className="flex-1">
                      <strong>{e.label}</strong>
                      <p className="text-xs text-gray-600 mt-1">{e.desc}</p>
                    </div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Valor */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-2">Valor *</label>
              <div className="flex">
                <input type="number" step="0.01" className="input-field rounded-r-none"
                  placeholder={seleccion?.placeholder}
                  value={valor} onChange={e => setValor(e.target.value)} required />
                {seleccion?.suffix && (
                  <span className="bg-gray-200 px-3 flex items-center rounded-r border border-l-0 border-gray-300">
                    {seleccion.suffix}
                  </span>
                )}
              </div>
            </div>

            {/* Lista origen (solo si AJUSTE_SOBRE_LISTA) */}
            {estrategia === 'AJUSTE_SOBRE_LISTA' && (
              <div>
                <label className="block font-semibold mb-2">Lista origen *</label>
                <select className="input-field"
                  value={listaOrigenId} onChange={e => setListaOrigenId(e.target.value)} required>
                  <option value="">Seleccionar...</option>
                  {(otrasListas || []).filter(l => l.id !== lista.id).map(l => (
                    <option key={l.id} value={l.id}>{l.nombre} ({l.tipoCliente})</option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Filtros opcionales */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 bg-white p-3 rounded border">
            <p className="md:col-span-2 text-sm text-gray-600 flex items-center gap-1">
              <FaInfoCircle /> Filtros opcionales (déjalos vacíos para aplicar a TODOS los productos):
            </p>
            <div>
              <label className="block text-sm mb-1">Solo categorias:</label>
              <select multiple className="input-field h-32"
                value={categoriaIds} onChange={e => setCategoriaIds(Array.from(e.target.selectedOptions, o => o.value))}>
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
              <p className="text-xs text-gray-500 mt-1">Mantén Ctrl/Cmd para seleccionar varias</p>
            </div>
            <div>
              <label className="block text-sm mb-1">Solo marcas:</label>
              <select multiple className="input-field h-32"
                value={marcaIds} onChange={e => setMarcaIds(Array.from(e.target.selectedOptions, o => o.value))}>
                {marcas.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
              </select>
            </div>
          </div>

          {/* Sobrescribir */}
          <label className="flex items-start gap-2 bg-yellow-50 border border-yellow-200 p-3 rounded">
            <input type="checkbox" checked={sobrescribir}
              onChange={e => setSobrescribir(e.target.checked)} className="mt-1" />
            <div>
              <strong>Sobrescribir precios existentes</strong>
              <p className="text-sm text-gray-600">
                Por defecto solo se crean precios para productos que aún NO los tienen en esta lista.
                Si marcas esto, los precios vigentes se cerrarán y se reemplazarán por los nuevos (queda historial).
              </p>
            </div>
          </label>

          {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded">{error}</div>}

          {result && (
            <div className="bg-green-50 border border-green-300 p-4 rounded">
              <p className="font-bold text-green-800 mb-2">Generacion completada:</p>
              <ul className="text-sm space-y-1">
                <li><strong>{result.creados}</strong> precios creados</li>
                {result.actualizados > 0 && <li><strong>{result.actualizados}</strong> precios actualizados</li>}
                {result.omitidos > 0 && <li><strong>{result.omitidos}</strong> productos omitidos (ya tenían precio)</li>}
                {result.sinPrecioCompra > 0 && <li><strong>{result.sinPrecioCompra}</strong> productos sin precio de compra registrado</li>}
                {result.sinPrecioEnOrigen > 0 && <li><strong>{result.sinPrecioEnOrigen}</strong> productos sin precio en la lista origen</li>}
              </ul>
            </div>
          )}

          <button type="submit" disabled={submitting}
            className="bg-purple-700 hover:bg-purple-800 text-white font-medium py-2 px-6 rounded-lg flex items-center gap-2">
            <FaMagic /> {submitting ? 'Generando...' : 'Generar precios'}
          </button>
        </form>
      )}
    </div>
  );
};

export default BulkPricing;
