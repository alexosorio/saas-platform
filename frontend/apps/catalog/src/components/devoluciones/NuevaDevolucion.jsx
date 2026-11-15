import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FaUndo, FaSearch, FaArrowLeft } from 'react-icons/fa';
import devolucionService from '../../services/devolucionService';
import orderService from '../../services/orderService';
import DevolucionModal from '../orders/DevolucionModal';

const money = (n) => '$' + Number(n || 0).toLocaleString('es-CO', { maximumFractionDigits: 0 });

/** Nueva devolución: buscar la factura (o el cliente) → elegirla → registrar la devolución. */
export default function NuevaDevolucion() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [facturas, setFacturas] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [orden, setOrden] = useState(null);
  const [previas, setPrevias] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    if (q.trim().length < 2) { setFacturas([]); return; }
    const t = setTimeout(async () => {
      setBuscando(true);
      try { setFacturas(await devolucionService.buscarFacturas(q.trim())); }
      catch { setFacturas([]); } finally { setBuscando(false); }
    }, 300);
    return () => clearTimeout(t);
  }, [q]);

  const elegir = async (id) => {
    setError('');
    try {
      const [o, dv] = await Promise.all([orderService.getById(id), orderService.getDevoluciones(id)]);
      setOrden(o); setPrevias(dv);
    } catch (e) { setError(e.response?.data?.message || 'No se pudo cargar la factura'); }
  };

  return (
    <div className="max-w-4xl">
      <Link to="/catalog/devoluciones" className="text-gray-500 mb-4 inline-flex items-center gap-2"><FaArrowLeft /> Devoluciones</Link>
      <h1 className="text-3xl font-bold flex items-center gap-3 mb-2"><FaUndo className="text-primary-600" /> Nueva devolución</h1>
      <p className="text-sm text-gray-500 mb-5">Busca la factura por su número o por el nombre o documento del cliente.</p>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <div className="card mb-4">
        <div className="relative">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input className="input-field pl-9" autoFocus placeholder="Ej: ORD-2026-0153, Juan Pérez o 1047…" value={q} onChange={e => setQ(e.target.value)} />
        </div>
        {buscando && <p className="text-sm text-gray-400 mt-3">Buscando…</p>}
        {!buscando && q.trim().length >= 2 && facturas.length === 0 && (
          <p className="text-sm text-gray-500 mt-3">No hay facturas confirmadas que coincidan.</p>
        )}
        {facturas.length > 0 && (
          <div className="divide-y mt-3">
            {facturas.map(fa => {
              const disponible = Number(fa.total) - Number(fa.montoDevuelto || 0);
              return (
                <button key={fa.id} type="button" onClick={() => elegir(fa.id)} disabled={disponible <= 0}
                  className="w-full text-left py-3 px-2 hover:bg-gray-50 rounded flex justify-between gap-3 disabled:opacity-50">
                  <span><strong>{fa.numero}</strong> · {fa.cliente || 'Cliente ocasional'}
                    {fa.documento && <span className="text-gray-500"> ({fa.documento})</span>}
                    <span className="block text-xs text-gray-500">{String(fa.fecha || '').slice(0, 10)} · {fa.estado}</span></span>
                  <span className="text-right text-sm"><span className="font-mono">{money(fa.total)}</span>
                    {Number(fa.montoDevuelto) > 0 && <span className="block text-xs text-amber-700">Devuelto {money(fa.montoDevuelto)}</span>}
                    {disponible <= 0 && <span className="block text-xs text-gray-500">Totalmente devuelta</span>}</span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {orden && (
        <DevolucionModal order={orden} devoluciones={previas} onClose={() => setOrden(null)}
          onDone={(r) => navigate(r?.id ? `/catalog/devoluciones/${r.id}` : '/catalog/devoluciones')} />
      )}
    </div>
  );
}
