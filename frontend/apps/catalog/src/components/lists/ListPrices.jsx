import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { useParams, useNavigate } from 'react-router-dom';
import { FaArrowLeft, FaCalculator, FaSearch, FaEdit, FaExclamationTriangle } from 'react-icons/fa';
import listService from '../../services/listService';
import ShareLinks from './ShareLinks';
import ColorChips, { rangoPrecio } from '../common/ColorChips';

const ListPrices = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lista, setLista] = useState(null);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => { load(); }, [id]);

  const load = async () => {
    try {
      setLoading(true);
      const [l, computed] = await Promise.all([
        listService.getById(id), listService.computedPrices(id),
      ]);
      setLista(l); setItems(computed);
    } catch (e) { console.error(e); } finally { setLoading(false); }
  };

  const filtered = search
    ? items.filter(it =>
        (it.nombre || '').toLowerCase().includes(search.toLowerCase()) ||
        (it.sku || '').toLowerCase().includes(search.toLowerCase()))
    : items;

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(filtered, { clave: 'precios-lista', reiniciar: search });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  if (!lista) return <div className="text-center py-10 text-gray-500">Lista no encontrada</div>;

  const baseLabel = lista.baseCalculo === 'precio_venta' ? 'precio de venta' : 'precio de compra';
  const formulaTexto = lista.tipoAjuste === 'valor_fijo'
    ? `${baseLabel} + $${Number(lista.valorAjuste).toLocaleString()}`
    : `${baseLabel} ${Number(lista.valorAjuste) < 0 ? '−' : '+'} ${Math.abs(Number(lista.valorAjuste))}%`;

  const sinUnidades = items.filter(it => !it.unidadesDisponibles && !it.esOverride).length;
  const sinPrecio = items.filter(it => it.precioCalculado == null && it.unidadesDisponibles > 0).length;
  const overrides = items.filter(it => it.esOverride).length;

  const limpiarOverrides = async () => {
    if (!window.confirm(
      `Hay ${overrides} producto(s) con precio manual (override) que está tapando la fórmula ` +
      `"${formulaTexto}". ¿Eliminar todos esos precios manuales para que aplique la fórmula? ` +
      `Esto borra las filas en la tabla "precios" de esta lista.`)) return;
    try {
      const r = await listService.clearOverrides(id);
      alert(r.message || 'Listo');
      load();
    } catch (e) {
      alert(e.response?.data?.message || 'Error al limpiar');
    }
  };

  return (
    <div>
      <button onClick={() => navigate('/catalog/lists')} className="text-gray-500 mb-4 flex items-center gap-2"><FaArrowLeft /> Volver a listas</button>

      {overrides > 0 && (
        <div className="bg-amber-50 border border-amber-300 rounded-lg px-4 py-3 mb-4">
          <p className="text-sm text-amber-900">
            <strong>{overrides}</strong> de {items.length} producto(s) muestran un <strong>precio manual</strong> (marcado con <span className="text-purple-600 font-bold">*</span>),
            no el calculado por la fórmula. Suele pasar por datos heredados del modelo anterior guardados en la tabla <code>precios</code>.
          </p>
          <button onClick={limpiarOverrides}
            className="mt-2 bg-amber-600 hover:bg-amber-700 text-white px-4 py-2 rounded text-sm font-semibold">
            Quitar precios manuales y usar la fórmula
          </button>
        </div>
      )}

      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-3xl font-bold">{lista.nombre}</h1>
          <div className="flex items-center gap-2 mt-2 text-sm">
            <span className="px-2 py-1 bg-gray-100 rounded uppercase text-xs">{lista.tipoCliente}</span>
            <span className="flex items-center gap-1 text-primary-700"><FaCalculator /> {formulaTexto}</span>
          </div>
        </div>
        <button onClick={() => navigate(`/catalog/lists/${id}/edit`)} className="btn-secondary flex items-center gap-2">
          <FaEdit /> Editar fórmula
        </button>
      </div>

      {sinPrecio > 0 && (
        <div className="bg-yellow-50 border border-yellow-300 text-yellow-800 px-4 py-3 rounded mb-4 flex items-center gap-2">
          <FaExclamationTriangle />
          {sinPrecio} producto(s) tienen unidades sin {baseLabel}: complétalo en sus unidades para que la lista les calcule precio.
        </div>
      )}
      {sinUnidades > 0 && (
        <p className="text-sm text-gray-500 mb-4">
          {sinUnidades} producto(s) no tienen unidades disponibles, por eso no muestran precio. El precio sale de cada unidad.
        </p>
      )}

      <div className="card mb-4">
        <div className="flex items-center gap-3">
          <FaSearch className="text-gray-400" />
          <input className="input-field flex-1" placeholder="Buscar por nombre o SKU..."
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
      </div>

      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Producto</th>
            <th className="px-4 py-3 text-left">SKU</th>
            <th className="px-4 py-3 text-left">Marca</th>
            <th className="px-4 py-3 text-left">Colores</th>
            <th className="px-4 py-3 text-right">Precio en lista</th>
            <th className="px-4 py-3 text-center">Stock</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(it => (
              <tr key={it.productoId} className={`border-b ${it.precioCalculado == null && it.unidadesDisponibles > 0 ? 'bg-yellow-50' : ''}`}>
                <td className="px-4 py-3">{it.nombre}</td>
                <td className="px-4 py-3 font-mono text-sm">{it.sku || '-'}</td>
                <td className="px-4 py-3 text-sm">{it.marcaNombre || '-'}</td>
                <td className="px-4 py-3"><ColorChips colores={it.colores} max={4} size="xs" vacio={<span className="text-gray-300">—</span>} /></td>
                <td className="px-4 py-3 text-right font-mono font-bold text-primary-600 whitespace-nowrap">
                  {it.precioCalculado != null
                    ? rangoPrecio(it.precioMinimo, it.precioMaximo)
                    : <span className="text-sm font-normal text-gray-400">{it.unidadesDisponibles > 0 ? 'sin precio base' : 'sin unidades'}</span>}
                  {it.esOverride && <span className="ml-1 text-xs text-purple-600" title="Precio manual (override)">*</span>}
                </td>
                <td className="px-4 py-3 text-center">{it.stockTotal}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {filtered.length === 0 && <div className="text-center py-8 text-gray-500">
          {items.length === 0 ? 'No hay productos activos. Crea productos para que aparezcan aquí.' : 'Sin resultados para esa búsqueda'}
        </div>}
        <Paginacion {...pg.paginador} />
      </div>

      <p className="text-xs text-gray-500 mt-2">
        Esta lista muestra <strong>todos los productos activos</strong> con su precio calculado por la fórmula.
        El asterisco (*) indica un precio fijado manualmente que sobreescribe la fórmula.
      </p>

      <ShareLinks listaId={id} />
    </div>
  );
};

export default ListPrices;
