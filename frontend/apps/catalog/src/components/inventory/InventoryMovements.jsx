import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { useParams, useNavigate } from 'react-router-dom';
import { FaArrowLeft } from 'react-icons/fa';
import inventoryService from '../../services/inventoryService';

const tipoColor = {
  entrada: 'bg-green-100 text-green-800', salida: 'bg-red-100 text-red-800',
  ajuste: 'bg-primary-100 text-primary-800', transferencia: 'bg-purple-100 text-purple-800',
};

const InventoryMovements = () => {
  const { inventarioId } = useParams();
  const navigate = useNavigate();
  const [movs, setMovs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { (async () => {
    try { setMovs(await inventoryService.getMovements(inventarioId)); } catch (e) { console.error(e); } finally { setLoading(false); }
  })(); }, [inventarioId]);

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(movs, { clave: 'movimientos' });
  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;
  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/catalog/inventory')} className="text-primary-600"><FaArrowLeft size={22} /></button>
        <h1 className="text-3xl font-bold">Movimientos de Inventario</h1>
      </div>
      <div className="card p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Fecha</th><th className="px-4 py-3 text-left">Tipo</th>
            <th className="px-4 py-3 text-center">Cantidad</th><th className="px-4 py-3 text-center">Anterior</th>
            <th className="px-4 py-3 text-center">Nueva</th><th className="px-4 py-3 text-left">Motivo</th>
          </tr></thead>
          <tbody>
            {pg.visibles.map(m => (
              <tr key={m.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 text-sm">{m.createdAt ? new Date(m.createdAt).toLocaleString() : '-'}</td>
                <td className="px-4 py-3"><span className={`px-2 py-1 rounded text-xs ${tipoColor[m.tipo] || 'bg-gray-100'}`}>{m.tipo}</span></td>
                <td className="px-4 py-3 text-center font-semibold">{m.cantidad}</td>
                <td className="px-4 py-3 text-center text-gray-500">{m.cantidadAnterior}</td>
                <td className="px-4 py-3 text-center font-semibold text-primary-600">{m.cantidadNueva}</td>
                <td className="px-4 py-3 text-sm">{m.motivo || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {movs.length === 0 && <div className="text-center py-10 text-gray-500">Sin movimientos registrados</div>}
        <Paginacion {...pg.paginador} />
      </div>
    </div>
  );
};
export default InventoryMovements;
