import { useState, useEffect, useMemo } from 'react';
import { Autocomplete, lazyOptions, Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import { Link } from 'react-router-dom';
import { FaWarehouse, FaSlidersH, FaHistory, FaSearch } from 'react-icons/fa';
import warehouseService from '../../services/warehouseService';
import inventoryService from '../../services/inventoryService';

const InventoryList = () => {
  const bodegaSrc = useMemo(() => lazyOptions(() => warehouseService.getAll(), {
    label: (w) => `${w.nombre} (${w.codigo})`, sublabel: 'sedeNombre',
  }), []);
  const [selected, setSelected] = useState('');
  const [selectedLabel, setSelectedLabel] = useState('');
  const [texto, setTexto] = useState('');
  const [q, setQ] = useState('');
  useEffect(() => { const t = setTimeout(() => setQ(texto.trim()), 350); return () => clearTimeout(t); }, [texto]);

  useEffect(() => { (async () => {
    // Abre mostrando la primera bodega; la misma fuente cacheada alimenta al autocomplete
    try { const [primera] = await bodegaSrc.loadOptions('');
      if (primera) { setSelected(primera.value); setSelectedLabel(primera.label); } }
    catch (e) { console.error(e); }
  })(); }, []);

  // Solo se trae la página visible de la bodega elegida
  const pag = usePaginacionServidor(({ page, size }) => (selected
    ? inventoryService.porBodegaPaginado(selected, { page, size, ...(q ? { q } : {}) })
    : Promise.resolve([])), [selected, q], { clave: 'inventario' });
  const items = pag.items;
  const loading = pag.cargando && items.length === 0;

  const onSelect = (v, o) => { setSelected(v); setSelectedLabel(o?.label || ''); };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaWarehouse className="text-primary-600" /> Inventario</h1>
        <Link to="/catalog/inventory/adjust" className="btn-primary flex items-center gap-2"><FaSlidersH /> Ajustar Stock</Link>
      </div>

      <div className="card mb-4">
        <label className="block font-semibold mb-2">Bodega</label>
        <div className="max-w-md">
          <Autocomplete source={bodegaSrc} value={selected} valueLabel={selectedLabel}
            onChange={onSelect} placeholder="Buscar bodega…" />
        </div>
        {selected && (
          <div className="relative max-w-md mt-3">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={13} />
            <input className="input-field pl-9" placeholder="Producto, código, IMEI o código de barras…" value={texto} onChange={e => setTexto(e.target.value)} />
          </div>
        )}
      </div>

      <div className="card p-0 overflow-x-auto">
        {loading ? <div className="text-center py-10 text-gray-500">Cargando...</div> : (
          <table className="min-w-full">
            <thead className="bg-gray-100"><tr>
              <th className="px-4 py-3 text-left">Producto</th><th className="px-4 py-3 text-left">Unidad</th>
              <th className="px-4 py-3 text-center">Cantidad</th><th className="px-4 py-3 text-left">Ubicacion</th>
              <th className="px-4 py-3 text-right">Movimientos</th>
            </tr></thead>
            <tbody>
              {items.map(i => (
                <tr key={i.id} className="border-b hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{i.productoNombre}</td>
                  <td className="px-4 py-3 font-mono text-sm">{i.unidadCodigo}</td>
                  <td className="px-4 py-3 text-center"><span className="font-bold text-primary-600">{i.cantidad}</span></td>
                  <td className="px-4 py-3">{i.ubicacion || '-'}</td>
                  <td className="px-4 py-3 text-right">
                    <Link to={`/catalog/inventory/movements/${i.id}`} className="text-purple-600 inline-flex items-center gap-1"><FaHistory /> Ver</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="text-center py-10 text-gray-500">{!selected ? 'Elige una bodega' : q ? 'No hay unidades que coincidan' : 'Sin stock en esta bodega'}</div>}
        <Paginacion {...pag.paginador} />
      </div>
    </div>
  );
};
export default InventoryList;
