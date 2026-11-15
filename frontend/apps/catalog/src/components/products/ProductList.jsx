import { useState } from 'react';
import { Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaEdit, FaTrash, FaPlus, FaToggleOn, FaToggleOff, FaSearch, FaEye, FaBoxOpen, FaUpload } from 'react-icons/fa';
import productService from '../../services/productService';
import ColorChips, { rangoPrecio } from '../common/ColorChips';

const ProductList = () => {
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');   // búsqueda aplicada (al enviar el formulario)

  // Solo se trae la página visible; q busca por nombre, SKU o código de barras de sus unidades
  const pag = usePaginacionServidor(({ page, size }) => productService.paginado({ page, size, q }), [q], { clave: 'productos' });
  const products = pag.items;
  const loading = pag.cargando && products.length === 0 && !q;
  const error = pag.error;
  const load = pag.recargar;

  const handleSearch = (e) => { e.preventDefault(); setQ(search.trim()); };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este producto?')) return;
    try { await productService.delete(id); load(); }
    catch (err) { alert(err.response?.data?.message || 'Error al eliminar'); }
  };

  const handleToggle = async (p) => {
    try {
      if (p.activo) await productService.deactivate(p.id);
      else await productService.activate(p.id);
      load();
    } catch (err) { alert(err.response?.data?.message || 'Error'); }
  };

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando productos...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaBoxOpen className="text-primary-600" /> Productos</h1>
        <div className="flex gap-2">
          <Link to="/catalog/products/bulk" className="btn-secondary flex items-center gap-2">
            <FaUpload /> Carga masiva
          </Link>
          <Link to="/catalog/products/new" className="btn-primary flex items-center gap-2">
            <FaPlus /> Nuevo Producto
          </Link>
        </div>
      </div>

      {error && <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <form onSubmit={handleSearch} className="mb-4 flex gap-2">
        <input type="text" placeholder="Buscar por nombre, SKU o código de barras..." value={search}
          onChange={(e) => setSearch(e.target.value)} className="input-field flex-1" />
        <button type="submit" className="btn-secondary flex items-center gap-2"><FaSearch /> Buscar</button>
        {q && <button type="button" onClick={() => { setSearch(''); setQ(''); }} className="btn-secondary">Limpiar</button>}
      </form>

      <div className={`card p-0 overflow-x-auto transition-opacity ${pag.cargando ? 'opacity-60' : ''}`}>
        <table className="min-w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-4 py-3 text-left">SKU</th>
              <th className="px-4 py-3 text-left">Nombre</th>
              <th className="px-4 py-3 text-left">Categoria</th>
              <th className="px-4 py-3 text-left">Marca</th>
              <th className="px-4 py-3 text-left">Colores disponibles</th>
              <th className="px-4 py-3 text-right">Precio de venta</th>
              <th className="px-4 py-3 text-center">Stock</th>
              <th className="px-4 py-3 text-center">Estado</th>
              <th className="px-4 py-3 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-sm">{p.sku || '-'}</td>
                <td className="px-4 py-3 font-medium">{p.nombre}</td>
                <td className="px-4 py-3">
                  <span className="px-2 py-1 bg-primary-100 text-primary-800 rounded text-sm">{p.categoriaNombre || '-'}</span>
                </td>
                <td className="px-4 py-3">{p.marcaNombre || '-'}</td>
                <td className="px-4 py-3"><ColorChips colores={p.coloresDisponibles} max={4} size="xs" vacio={<span className="text-gray-300">—</span>} /></td>
                <td className="px-4 py-3 text-right font-mono text-sm whitespace-nowrap">{rangoPrecio(p.precioVentaDesde, p.precioVentaHasta)}</td>
                <td className="px-4 py-3 text-center">
                  <span className={`font-semibold ${p.stockTotal > 0 ? 'text-green-600' : 'text-gray-400'}`}>{p.stockTotal ?? 0}</span>
                </td>
                <td className="px-4 py-3 text-center">
                  <button onClick={() => handleToggle(p)} className={`text-2xl ${p.activo ? 'text-green-600' : 'text-gray-400'}`}
                    title={p.activo ? 'Activo' : 'Inactivo'}>
                    {p.activo ? <FaToggleOn /> : <FaToggleOff />}
                  </button>
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-3 text-lg">
                    <Link to={`/catalog/products/${p.id}`} className="text-purple-600 hover:text-purple-800" title="Ver detalle"><FaEye /></Link>
                    <Link to={`/catalog/products/${p.id}/edit`} className="text-primary-600 hover:text-primary-800" title="Editar"><FaEdit /></Link>
                    <button onClick={() => handleDelete(p.id)} className="text-red-600 hover:text-red-800" title="Eliminar"><FaTrash /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {products.length === 0 && !pag.cargando && (
          <div className="text-center py-10 text-gray-500">
            {q ? 'No hay productos que coincidan con la búsqueda.'
              : <>No hay productos. <Link to="/catalog/products/new" className="text-primary-600 underline">Crear el primero</Link></>}
          </div>
        )}
        <Paginacion {...pag.paginador} />
      </div>
    </div>
  );
};

export default ProductList;
