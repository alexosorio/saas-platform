import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { FaArrowLeft, FaEdit, FaPlus, FaBan, FaSearch, FaBarcode } from 'react-icons/fa';
import BajaUnidadModal from '../units/BajaUnidadModal';
import productService from '../../services/productService';
import unitService from '../../services/unitService';
import inventoryService from '../../services/inventoryService';
import ColorChips, { rangoPrecio } from '../common/ColorChips';

const estadoColor = {
  disponible: 'bg-green-100 text-green-800',
  vendido: 'bg-red-100 text-red-800',
  reservado: 'bg-yellow-100 text-yellow-800',
  dañado: 'bg-gray-200 text-gray-700',
  en_reparacion: 'bg-orange-100 text-orange-800',
};

const ProductDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [units, setUnits] = useState([]);
  const [stock, setStock] = useState(0);
  const [loading, setLoading] = useState(true);
  const [imei, setImei] = useState('');

  useEffect(() => { load(); }, [id]);

  const load = async () => {
    try {
      setLoading(true);
      const [p, u, s] = await Promise.all([
        productService.getById(id),
        unitService.getByProduct(id),
        inventoryService.getStock(id),
      ]);
      setProduct(p); setUnits(u); setStock(s.stock || 0);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  };

  const [bajaUnidad, setBajaUnidad] = useState(null);   // unidad seleccionada para dar de baja

  // Busca por IMEI, código único o código de barras (resalta todas las unidades que coinciden)
  const handleSearchImei = async (e) => {
    e.preventDefault();
    const q = imei.trim();
    if (!q) return;
    document.querySelectorAll('tr.bg-yellow-50').forEach((r) => r.classList.remove('bg-yellow-50'));
    const locales = units.filter((u) => [u.codigoBarras, u.codigoUnico, u.imei1, u.imei2].includes(q));
    if (locales.length) {
      // Ir a la página donde está la primera coincidencia y resaltarla cuando se dibuje
      pg.setPagina(Math.floor(units.findIndex((u) => u.id === locales[0].id) / pg.tamano));
      setTimeout(() => {
        locales.forEach((u) => document.getElementById(`unit-${u.id}`)?.classList.add('bg-yellow-50'));
        document.getElementById(`unit-${locales[0].id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 60);
      return;
    }
    try {
      const u = await unitService.searchByImei(q);
      alert(`IMEI encontrado en producto: ${u.productoNombre}`);
    } catch { alert('No hay unidades de este producto con ese IMEI, código o código de barras'); }
  };

  const asignarCodigoBarras = async (u) => {
    const nuevo = window.prompt(`Código de barras de la unidad ${u.codigoUnico}\n(escanéalo o escríbelo; déjalo vacío para quitarlo)`, u.codigoBarras || '');
    if (nuevo === null) return;
    try {
      const act = await unitService.setCodigoBarras(u.id, nuevo.trim());
      setUnits((l) => l.map((x) => (x.id === u.id ? { ...x, codigoBarras: act.codigoBarras } : x)));
    } catch (err) { alert(err.response?.data?.message || 'No se pudo guardar el código de barras'); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(units, { clave: 'unidades-producto' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  if (!product) return <div className="text-center py-10">Producto no encontrado</div>;

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/catalog/products')} className="text-primary-600"><FaArrowLeft size={22} /></button>
        <h1 className="text-3xl font-bold">{product.nombre}</h1>
        <Link to={`/catalog/products/${id}/edit`} className="ml-auto btn-primary flex items-center gap-2"><FaEdit /> Editar</Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <div className="card md:col-span-2">
          <h2 className="text-xl font-bold mb-4">Informacion</h2>
          <div className="grid grid-cols-2 gap-3">
            <p><strong>SKU:</strong> {product.sku || '-'}</p>
            <p><strong>Categoria:</strong> {product.categoriaNombre}</p>
            <p><strong>Marca:</strong> {product.marcaNombre || '-'}</p>
            <p><strong>Estado:</strong> <span className={`px-2 py-1 rounded text-sm ${product.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>{product.activo ? 'Activo' : 'Inactivo'}</span></p>
            <p><strong>Precio de venta:</strong> <span className="font-mono">{rangoPrecio(product.precioVentaDesde, product.precioVentaHasta, 'Sin unidades disponibles')}</span></p>
          </div>
          <div className="mt-3 pt-3 border-t">
            <p className="font-semibold mb-1.5">Colores disponibles</p>
            <ColorChips colores={product.coloresDisponibles} max={20}
              vacio={<span className="text-sm text-gray-400">Aún no hay unidades disponibles con color registrado.</span>} />
          </div>
          {product.descripcion && <p className="mt-3 pt-3 border-t text-gray-600">{product.descripcion}</p>}
        </div>
        <div className="card text-center">
          <h2 className="text-lg font-bold mb-2">Stock Total</h2>
          <div className="text-5xl font-bold text-primary-600 py-4">{stock}</div>
          <p className="text-gray-500">unidades</p>
        </div>
      </div>

      <div className="card p-0">
        <div className="p-6 border-b">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold">Unidades ({units.length})</h2>
            <Link to={`/catalog/products/${id}/units/new`} className="bg-green-600 hover:bg-green-700 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2">
              <FaPlus /> Nueva Unidad
            </Link>
          </div>
          <form onSubmit={handleSearchImei} className="flex gap-2">
            <input type="text" placeholder="Buscar por IMEI, código o código de barras..." value={imei} onChange={(e) => setImei(e.target.value)} className="input-field flex-1" />
            <button type="submit" className="btn-secondary flex items-center gap-2"><FaSearch /></button>
          </form>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left">Código / C. barras</th>
                <th className="px-4 py-3 text-left">IMEI 1</th>
                <th className="px-4 py-3 text-left">Condicion</th>
                <th className="px-4 py-3 text-center">Bateria</th>
                <th className="px-4 py-3 text-left">Color</th>
                <th className="px-4 py-3 text-right">P. Compra</th>
                <th className="px-4 py-3 text-right">P. Venta</th>
                <th className="px-4 py-3 text-center">Estado</th>
                <th className="px-4 py-3 text-left">Caracteristicas</th>
                <th className="px-4 py-3 text-right">Acc.</th>
              </tr>
            </thead>
            <tbody>
              {pg.visibles.map((u) => (
                <tr key={u.id} id={`unit-${u.id}`} className="border-b hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-sm">
                    {u.codigoUnico}
                    <button type="button" onClick={() => asignarCodigoBarras(u)} title={u.codigoBarras ? 'Cambiar código de barras' : 'Asignar código de barras'}
                      className={`flex items-center gap-1 text-xs mt-0.5 ${u.codigoBarras ? 'text-gray-600 hover:text-primary-700' : 'text-gray-400 hover:text-primary-700 font-sans'}`}>
                      <FaBarcode /> {u.codigoBarras || 'Asignar'}
                    </button>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs">{u.imei1 || '-'}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded text-xs ${u.condicion === 'nuevo' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>{u.condicion}</span>
                  </td>
                  <td className="px-4 py-3 text-center">{u.nivelBateria != null ? `${u.nivelBateria}%` : '-'}</td>
                  <td className="px-4 py-3">{u.color ? <ColorChips colores={[u.color]} size="xs" /> : <span className="text-gray-300">—</span>}</td>
                  <td className="px-4 py-3 text-right font-mono">${(u.precioCompra || 0).toLocaleString()}</td>
                  <td className="px-4 py-3 text-right font-mono">{u.precioVenta != null ? `$${Number(u.precioVenta).toLocaleString()}` : <span className="text-gray-300">—</span>}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`px-2 py-1 rounded text-xs ${u.estado === 'dado_de_baja' ? 'bg-gray-200 text-gray-600' : (estadoColor[u.estado] || 'bg-gray-100')}`}>
                      {u.estado === 'dado_de_baja' ? 'Dada de baja' : u.estado}
                    </span>
                    {u.estado === 'dado_de_baja' && u.motivoBaja && (
                      <div className="text-xs text-gray-500 mt-1 max-w-[16rem]" title={u.motivoBaja}>{u.motivoBaja}</div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-xs">
                    {u.caracteristicas && Object.entries(u.caracteristicas).map(([k, v]) => (
                      <div key={k}><strong>{k}:</strong> {v}</div>
                    ))}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {['disponible', 'dañado', 'en_reparacion'].includes(u.estado) && (
                      <button onClick={() => setBajaUnidad(u)} className="text-red-600 hover:text-red-800" title="Dar de baja"><FaBan /></button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {units.length === 0 && <div className="text-center py-8 text-gray-500">No hay unidades registradas</div>}
          <Paginacion {...pg.paginador} />
        </div>
      </div>
      {bajaUnidad && <BajaUnidadModal unidad={bajaUnidad} onClose={() => setBajaUnidad(null)} onDone={() => { setBajaUnidad(null); load(); }} />}
    </div>
  );
};

export default ProductDetail;
