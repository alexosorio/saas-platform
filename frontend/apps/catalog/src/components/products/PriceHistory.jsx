import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { useParams, useNavigate } from 'react-router-dom';
import { FaArrowLeft, FaHistory } from 'react-icons/fa';
import productService from '../../services/productService';

const PriceHistory = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [producto, setProducto] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { (async () => {
    try {
      const [h, p] = await Promise.all([productService.priceHistory(id), productService.getById(id)]);
      setItems(h); setProducto(p);
    } catch (e) { console.error(e); } finally { setLoading(false); }
  })(); }, [id]);

  const fmt = (v) => v != null ? `$${Number(v).toLocaleString()}` : '-';

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'historial-precios' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;

  return (
    <div className="max-w-4xl mx-auto">
      <button onClick={() => navigate(`/catalog/products/${id}`)} className="text-gray-500 mb-4 flex items-center gap-2"><FaArrowLeft /> Volver</button>
      <h1 className="text-3xl font-bold mb-2 flex items-center gap-3"><FaHistory className="text-primary-600" /> Historial de precios</h1>
      {producto && <p className="text-gray-600 mb-2">{producto.nombre} {producto.sku && `(${producto.sku})`}</p>}
      <p className="text-sm text-gray-500 bg-blue-50 p-3 rounded mb-6">
        Registro histórico de cuando los precios se definían en el producto. Ahora el precio de compra y de venta
        se registra en cada unidad.
      </p>

      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Fecha</th>
            <th className="px-4 py-3 text-right">Venta anterior</th>
            <th className="px-4 py-3 text-right">Venta nueva</th>
            <th className="px-4 py-3 text-right">Compra anterior</th>
            <th className="px-4 py-3 text-right">Compra nueva</th>
            <th className="px-4 py-3 text-left">Motivo</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(h => (
              <tr key={h.id} className="border-b">
                <td className="px-4 py-3 text-sm">{new Date(h.fechaCambio).toLocaleString()}</td>
                <td className="px-4 py-3 text-right font-mono text-gray-500">{fmt(h.precioVentaAnterior)}</td>
                <td className="px-4 py-3 text-right font-mono font-semibold">{fmt(h.precioVentaNuevo)}</td>
                <td className="px-4 py-3 text-right font-mono text-gray-500">{fmt(h.precioCompraAnterior)}</td>
                <td className="px-4 py-3 text-right font-mono font-semibold">{fmt(h.precioCompraNuevo)}</td>
                <td className="px-4 py-3 text-sm">{h.motivo || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-8 text-gray-500">Sin cambios de precio registrados</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};

export default PriceHistory;
