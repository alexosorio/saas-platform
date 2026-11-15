import { useState, useEffect, useRef, useMemo } from 'react';
import ColorChips from '../common/ColorChips';
import { Autocomplete, lazyOptions, remoteOptions } from '@saas/ui-shell';
import { useNavigate } from 'react-router-dom';
import { FaSave, FaTimes, FaTrash, FaShoppingCart, FaBarcode, FaExclamationTriangle, FaSearch, FaLock, FaStore, FaExchangeAlt } from 'react-icons/fa';
import orderService from '../../services/orderService';
import clientService from '../../services/clientService';
import listService from '../../services/listService';
import productService from '../../services/productService';
import sedeService from '../../services/sedeService';

const OrderForm = () => {
  const navigate = useNavigate();
  const scanRef = useRef(null);

  // Bajo demanda: clientes se buscan en el servidor; listas de precios se cargan al abrir el campo
  const clienteSrc = useMemo(() => remoteOptions((q) => clientService.search(q), {
    label: (c) => `${c.nombre} (${c.tipo})`, sublabel: 'documento',
  }), []);
  const listaSrc = useMemo(() => lazyOptions(() => listService.getActive(), {
    label: (l) => `${l.nombre} (${l.tipoCliente})`,
  }), []);
  const [listaLabel, setListaLabel] = useState('');

  // Sede: arranca con la del usuario; solo quien tiene permiso puede elegir otra (autocomplete)
  const [ctxVenta, setCtxVenta] = useState(null);   // { sedeId, sedeNombre, puedeCambiar, sedesPermitidas }
  const [sedeLabel, setSedeLabel] = useState('');
  const [avisoSede, setAvisoSede] = useState(null); // unidad encontrada en otra sede: { sedeId, sedeNombre, bodegaNombre }
  const sedeSrc = useMemo(() => lazyOptions(() => sedeService.getActive(), {
    label: 'nombre', sublabel: 'ciudadNombre',
    filter: (s) => !ctxVenta?.sedesPermitidas || ctxVenta.sedesPermitidas.includes(s.id),
  }), [ctxVenta]);
  const [error, setError] = useState('');
  const [scanError, setScanError] = useState('');
  const [elegirColor, setElegirColor] = useState(null);   // { codigo, productoNombre, colores }
  const [loading, setLoading] = useState(false);

  const [header, setHeader] = useState({ clienteId: '', sedeId: '', listaPreciosId: '', descuento: '', notas: '' });
  const [items, setItems] = useState([]);
  const [scanCode, setScanCode] = useState('');
  // Autocomplete on-demand (respaldo si falla la pistola)
  const [q, setQ] = useState('');
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [showAuto, setShowAuto] = useState(false);

  useEffect(() => {
    const term = q.trim();
    if (term.length < 2) { setResults([]); return; }
    setSearching(true);
    const t = setTimeout(async () => {
      try { setResults(await productService.search(term)); }
      catch { setResults([]); }
      finally { setSearching(false); }
    }, 350);
    return () => clearTimeout(t);
  }, [q]);

  const pickProducto = async (p) => {
    setShowAuto(false); setQ('');
    // Reutiliza el flujo de scan usando el SKU del producto (hereda precio y validación de stock)
    await agregarPorCodigo(p.sku || p.codigo || String(p.id));
  };

  useEffect(() => {
    setTimeout(() => scanRef.current?.focus(), 200);
  }, []);

  useEffect(() => {
    orderService.contextoVenta()
      .then((c) => {
        setCtxVenta(c);
        if (c.sedeId) {
          setHeader(h => (h.sedeId ? h : { ...h, sedeId: c.sedeId }));
          setSedeLabel(c.sedeNombre || '');
        }
      })
      .catch(() => setCtxVenta({ sedeId: null, puedeCambiar: true, sedesPermitidas: null }));
  }, []);

  const cambiarSede = (id, label) => {
    setHeader(h => ({ ...h, sedeId: id || '' }));
    setSedeLabel(label || '');
    setAvisoSede(null);
  };

  // Al elegir cliente se sugiere la lista de precios de su tipo (mayorista/minorista)
  const onChangeCliente = async (id, opcion) => {
    setHeader(h => ({ ...h, clienteId: id }));
    const tipo = opcion?.raw?.tipo;
    if (!tipo || header.listaPreciosId) return;
    try {
      const listas = await listaSrc.loadOptions('');
      const sugerida = listas.find(l => l.raw?.tipoCliente === tipo);
      if (sugerida) {
        setHeader(h => (h.listaPreciosId ? h : { ...h, listaPreciosId: sugerida.value }));
        setListaLabel(sugerida.label);
      }
    } catch (e) { console.error(e); }
  };

  const agregarPorCodigo = async (codigo, color) => {
    setScanError(''); setElegirColor(null);
    if (!codigo || !codigo.trim()) return;
    if (!header.listaPreciosId) {
      setScanError('Selecciona primero una Lista de Precios');
      return;
    }
    try {
      const r = await orderService.scan(codigo.trim(), header.listaPreciosId, {
        color, excluir: items.map(it => it.unidadId), sedeId: header.sedeId,
      });

      // Buscado por producto (SKU / código de barras) con varios colores: preguntar cuál quiere el cliente
      if (r.tipoEncontrado === 'producto' && !color && (r.coloresDisponibles?.length || 0) > 1) {
        setElegirColor({ codigo: codigo.trim(), productoNombre: r.productoNombre, colores: r.coloresDisponibles });
        setScanCode('');
        return;
      }

      if (r.tipoEncontrado === 'unidad' && r.estado && r.estado !== 'disponible') {
        setScanError(`Unidad ${r.unidadCodigo} no disponible (estado: ${r.estado})`);
        setScanCode(''); return;
      }
      if (!r.unidadId) {
        setScanError(`Producto "${r.productoNombre}" no tiene unidades disponibles (stock: ${r.stockTotal})`);
        setScanCode(''); return;
      }
      if (items.find(it => it.unidadId === r.unidadId)) {
        setScanError(`La unidad ${r.unidadCodigo} ya esta en la orden`);
        setScanCode(''); return;
      }
      if (!r.precioSugerido) {
        setScanError(`La unidad ${r.unidadCodigo} de "${r.productoNombre}" no tiene precio base para esta lista. Ajústalo manualmente.`);
      }

      // La unidad está en otra sede: se avisa y, si tiene permiso, puede pasar la orden a esa sede
      const otraSede = r.sedeId && header.sedeId && String(r.sedeId) !== String(header.sedeId);
      setAvisoSede(otraSede ? { sedeId: r.sedeId, sedeNombre: r.sedeNombre, bodegaNombre: r.bodegaNombre } : null);

      setItems(prev => [...prev, {
        unidadId: r.unidadId,
        unidadCodigo: r.unidadCodigo,
        sedeId: r.sedeId || null,
        sedeNombre: r.sedeNombre || null,
        bodegaNombre: r.bodegaNombre || null,
        productoNombre: r.productoNombre,
        color: r.color || null,
        cantidad: 1,
        precioUnitario: r.precioSugerido ? Number(r.precioSugerido) : 0,
        descuentoLinea: 0,
        sinPrecio: !r.precioSugerido,
      }]);
      setScanCode('');
      scanRef.current?.focus();
    } catch (err) {
      setScanError(err.response?.data?.message || 'Codigo no encontrado');
      setScanCode('');
    }
  };

  const onScan = (e) => { e.preventDefault(); agregarPorCodigo(scanCode); };

  const updateItem = (idx, patch) => {
    setItems(items.map((it, i) => i === idx ? { ...it, ...patch } : it));
  };
  const removeItem = (idx) => setItems(items.filter((_, i) => i !== idx));

  const subtotal = items.reduce((s, it) => s + (Number(it.precioUnitario) * Number(it.cantidad) - Number(it.descuentoLinea || 0)), 0);
  const descuento = parseFloat(header.descuento) || 0;
  const total = subtotal - descuento;

  const submit = async (e) => {
    e.preventDefault(); setError('');
    if (!header.sedeId) { setError('Selecciona la sede de la orden'); return; }
    if (items.length === 0) { setError('Agrega al menos un item escaneando un codigo'); return; }
    if (items.some(it => !it.precioUnitario || it.precioUnitario <= 0)) {
      setError('Hay items sin precio. Completalos antes de crear la orden.'); return;
    }
    setLoading(true);
    try {
      const orden = await orderService.create({
        clienteId: parseInt(header.clienteId), sedeId: parseInt(header.sedeId),
        descuento, notas: header.notas || null,
        items: items.map(it => ({
          unidadId: it.unidadId, cantidad: Number(it.cantidad),
          precioUnitario: Number(it.precioUnitario),
          descuentoLinea: Number(it.descuentoLinea || 0),
        })),
      });
      navigate(`/catalog/orders/${orden.id}`);
    } catch (err) { setError(err.response?.data?.message || 'Error al crear la orden'); }
    finally { setLoading(false); }
  };

  return (
    <div className="max-w-5xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 flex items-center gap-3"><FaShoppingCart className="text-primary-600" /> Nueva Orden</h1>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <div className="card mb-4">
        <h2 className="font-bold mb-3">Datos generales</h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block font-semibold mb-2">Cliente *</label>
            <Autocomplete source={clienteSrc} value={header.clienteId}
              onChange={onChangeCliente} placeholder="Nombre, documento o email…" required />
          </div>
          <div>
            <label className="block font-semibold mb-2">Lista de Precios *</label>
            <Autocomplete source={listaSrc} value={header.listaPreciosId} valueLabel={listaLabel}
              onChange={(v, o) => { setHeader(h => ({ ...h, listaPreciosId: v })); setListaLabel(o?.label || ''); }}
              placeholder="Buscar lista de precios…" required />
          </div>
          <div>
            <label className="block font-semibold mb-2">Sede *</label>
            {ctxVenta?.puedeCambiar ? (
              <Autocomplete source={sedeSrc} value={header.sedeId} valueLabel={sedeLabel}
                onChange={(v, o) => cambiarSede(v, o?.label)}
                placeholder="Buscar sede…" required />
            ) : (
              <div className="input-field flex items-center gap-2 bg-gray-50 text-gray-700 cursor-not-allowed"
                title="Para facturar en otra sede necesitas el permiso de vender desde otras bodegas">
                <FaLock className="text-gray-400 shrink-0" />
                <span className="truncate">{ctxVenta ? (sedeLabel || 'Sin sede asignada') : 'Cargando…'}</span>
              </div>
            )}
            {ctxVenta && !ctxVenta.puedeCambiar && (
              <p className="text-xs text-gray-500 mt-1">Tu sede. Cambiarla requiere permiso.</p>
            )}
            {ctxVenta && !ctxVenta.sedeId && header.sedeId === '' && (
              <p className="text-xs text-amber-600 mt-1">No tienes sede asignada: elige una.</p>
            )}
          </div>
          <div>
            <label className="block font-semibold mb-2">Descuento global</label>
            <input type="number" step="0.01" className="input-field" value={header.descuento}
              onChange={e => setHeader({...header, descuento: e.target.value})} />
          </div>
        </div>
      </div>

      <div className="card mb-4 bg-blue-50 border border-blue-200">
        <h2 className="font-bold mb-3 flex items-center gap-2">
          <FaBarcode className="text-primary-700" /> Escanear codigo
        </h2>
        <form onSubmit={onScan} className="flex gap-2">
          <input ref={scanRef} autoFocus
            type="text" placeholder="Escanea el codigo de barras, SKU o IMEI y pulsa Enter"
            className="input-field flex-1 font-mono text-lg"
            value={scanCode} onChange={e => setScanCode(e.target.value)} />
          <button type="submit" className="btn-primary px-6">Agregar</button>
        </form>

        <div className="mt-3 relative">
          <p className="text-xs text-gray-500 mb-1 flex items-center gap-1"><FaSearch /> ¿Falló la pistola? Busca el producto:</p>
          <input type="text" placeholder="Escribe nombre o SKU (mín. 2 caracteres)..."
            className="input-field w-full"
            value={q}
            onChange={e => { setQ(e.target.value); setShowAuto(true); }}
            onFocus={() => setShowAuto(true)} />
          {showAuto && q.trim().length >= 2 && (
            <div className="absolute z-20 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto">
              {searching && <div className="px-3 py-2 text-sm text-gray-400">Buscando…</div>}
              {!searching && results.length === 0 && <div className="px-3 py-2 text-sm text-gray-400">Sin resultados</div>}
              {results.map(p => (
                <button type="button" key={p.id} onClick={() => pickProducto(p)}
                  className="w-full text-left px-3 py-2 hover:bg-primary-50 flex justify-between items-center gap-2 border-b border-gray-50 last:border-0">
                  <span>
                    <span className="font-medium">{p.nombre}</span>
                    <span className="text-xs text-gray-400 ml-2 font-mono">{p.sku}</span>
                  </span>
                  <span className="text-xs text-gray-500 whitespace-nowrap">
                    {p.stockTotal != null ? `stock ${p.stockTotal}` : ''}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
        {elegirColor && (
          <div className="mt-3 border border-primary-200 bg-primary-50 rounded-lg p-3">
            <p className="text-sm mb-2">¿Qué color de <strong>{elegirColor.productoNombre}</strong> quiere el cliente?</p>
            <div className="flex flex-wrap gap-2">
              {elegirColor.colores.map(c => (
                <button key={c} type="button" onClick={() => agregarPorCodigo(elegirColor.codigo, c)}
                  className="px-3 py-1.5 rounded-full border border-gray-300 bg-white text-sm hover:border-primary-500 hover:text-primary-700">
                  {c}
                </button>
              ))}
              <button type="button" onClick={() => setElegirColor(null)} className="px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700">Cancelar</button>
            </div>
          </div>
        )}
        {avisoSede && (
          <div className="mt-2 bg-amber-50 border border-amber-200 text-amber-800 px-3 py-2 rounded flex flex-wrap items-center gap-2 text-sm">
            <FaStore className="shrink-0" />
            <span>La unidad está en <strong>{avisoSede.sedeNombre}</strong>{avisoSede.bodegaNombre ? ` (bodega ${avisoSede.bodegaNombre})` : ''}, no en la sede de la orden.</span>
            {ctxVenta?.puedeCambiar && (!ctxVenta.sedesPermitidas || ctxVenta.sedesPermitidas.includes(avisoSede.sedeId)) && (
              <button type="button" onClick={() => cambiarSede(avisoSede.sedeId, avisoSede.sedeNombre)}
                className="ml-auto flex items-center gap-1 px-2 py-1 rounded border border-amber-300 bg-white hover:bg-amber-100">
                <FaExchangeAlt /> Facturar desde {avisoSede.sedeNombre}
              </button>
            )}
          </div>
        )}
        {scanError && (
          <div className="mt-2 bg-red-100 text-red-700 px-3 py-2 rounded flex items-center gap-2">
            <FaExclamationTriangle /> {scanError}
          </div>
        )}
        <p className="text-xs text-gray-600 mt-2">
          Los lectores USB se comportan como teclado: el cursor debe estar en este campo (ya esta enfocado). El precio se trae de la lista seleccionada.
        </p>
      </div>

      <div className="card mb-4 p-0 overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-100"><tr>
            <th className="px-4 py-3 text-left">Producto</th>
            <th className="px-4 py-3 text-left">Unidad</th>
            <th className="px-4 py-3 text-center">Cant.</th>
            <th className="px-4 py-3 text-right">P. Unit.</th>
            <th className="px-4 py-3 text-right">Desc.</th>
            <th className="px-4 py-3 text-right">Subtotal</th>
            <th></th>
          </tr></thead>
          <tbody>
            {items.map((it, idx) => (
              <tr key={idx} className={`border-b ${it.sinPrecio ? 'bg-yellow-50' : ''}`}>
                <td className="px-4 py-3">
                  {it.productoNombre}
                  {it.color && <div className="mt-1"><ColorChips colores={[it.color]} size="xs" /></div>}
                </td>
                <td className="px-4 py-3 font-mono text-sm">
                  {it.unidadCodigo}
                  {it.sedeNombre && (
                    <div className={`font-sans text-xs mt-0.5 ${String(it.sedeId) !== String(header.sedeId) ? 'text-amber-600' : 'text-gray-400'}`}>
                      {it.sedeNombre}{it.bodegaNombre ? ` · ${it.bodegaNombre}` : ''}
                    </div>
                  )}
                </td>
                <td className="px-4 py-3 text-center">
                  <input type="number" min="1" className="input-field w-16 text-center"
                    value={it.cantidad} onChange={e => updateItem(idx, { cantidad: e.target.value })} />
                </td>
                <td className="px-4 py-3 text-right">
                  <input type="number" step="0.01" className={`input-field w-28 text-right font-mono ${it.sinPrecio ? 'border-yellow-500' : ''}`}
                    value={it.precioUnitario}
                    onChange={e => updateItem(idx, { precioUnitario: e.target.value, sinPrecio: false })} />
                </td>
                <td className="px-4 py-3 text-right">
                  <input type="number" step="0.01" className="input-field w-24 text-right"
                    value={it.descuentoLinea}
                    onChange={e => updateItem(idx, { descuentoLinea: e.target.value })} />
                </td>
                <td className="px-4 py-3 text-right font-mono">
                  ${(Number(it.precioUnitario) * Number(it.cantidad) - Number(it.descuentoLinea || 0)).toLocaleString()}
                </td>
                <td className="px-4 py-3 text-right">
                  <button onClick={() => removeItem(idx)} className="text-red-600"><FaTrash /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <div className="text-center py-8 text-gray-500">Escanea un codigo para agregar items</div>}
      </div>

      <div className="card mb-4">
        <div className="flex justify-end gap-8 text-right">
          <div><p className="text-gray-500">Subtotal</p><p className="text-xl font-mono">${subtotal.toLocaleString()}</p></div>
          <div><p className="text-gray-500">Descuento</p><p className="text-xl font-mono text-red-600">-${descuento.toLocaleString()}</p></div>
          <div><p className="text-gray-500">Total</p><p className="text-2xl font-bold font-mono text-primary-600">${total.toLocaleString()}</p></div>
        </div>
        <div className="mt-4">
          <label className="block font-semibold mb-2">Notas</label>
          <textarea className="input-field" rows="2" value={header.notas}
            onChange={e => setHeader({...header, notas: e.target.value})} />
        </div>
      </div>

      <div className="flex gap-3">
        <button onClick={submit} disabled={loading} className="btn-primary flex items-center gap-2">
          <FaSave /> {loading ? 'Creando...' : 'Crear Orden (borrador)'}
        </button>
        <button onClick={() => navigate('/catalog/orders')} className="btn-secondary flex items-center gap-2">
          <FaTimes /> Cancelar
        </button>
      </div>
    </div>
  );
};

export default OrderForm;
