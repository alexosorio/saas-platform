import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { FaCrown, FaCheck, FaTimes as FaX, FaExternalLinkAlt } from 'react-icons/fa';
import admin from '../../services/adminSubscriptionService';
import { AdminTabs, Badge, ESTADO_PAGO, MotivoModal, money, fecha } from './adminUi';

const CATALOG_API = import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8083';
const urlSoporte = (u) => (!u ? null : u.startsWith('http') ? u : CATALOG_API + u);

export default function AdminPagos() {
  const [estado, setEstado] = useState('pendiente');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rechazar, setRechazar] = useState(null);
  const [procesando, setProcesando] = useState(null);

  const cargar = async () => {
    setLoading(true); setError('');
    try { setItems(await admin.pagos(estado)); }
    catch (e) { setError(e.response?.status === 403 ? 'No tienes permiso de administración de la plataforma.' : (e.response?.data?.message || 'Error al cargar')); }
    finally { setLoading(false); }
  };
  useEffect(() => { cargar(); /* eslint-disable-next-line */ }, [estado]);

  const aprobar = async (p) => {
    if (!window.confirm(`¿Aprobar el pago de ${p.organizacionNombre} por ${money(p.monto, p.moneda)}? La suscripción se activará y su vigencia se extenderá un período.`)) return;
    setProcesando(p.id);
    try { await admin.aprobarPago(p.id); cargar(); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo aprobar'); }
    finally { setProcesando(null); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'admin-pagos', reiniciar: estado });
  return (
    <div>
      <h1 className="text-3xl font-bold flex items-center gap-3 mb-4"><FaCrown className="text-primary-600" /> Administración de suscripciones</h1>
      <AdminTabs />
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <div className="flex flex-wrap gap-2 mb-4">
        {['pendiente', 'completado', 'fallido', ''].map(e => (
          <button key={e} onClick={() => setEstado(e)}
            className={`px-3 py-1 rounded text-sm ${estado === e ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700'}`}>
            {e === '' ? 'Todos' : ESTADO_PAGO[e][0]}
          </button>
        ))}
      </div>

      <div className="card overflow-x-auto">
        {loading ? <div className="py-8 text-center text-gray-500">Cargando…</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b">
              <th className="py-2 px-3">Organización</th><th className="py-2 px-3 text-right">Monto</th><th className="py-2 px-3">Pago</th>
              <th className="py-2 px-3">Soporte</th><th className="py-2 px-3">Estado</th><th className="py-2 px-3 text-right">Validación</th>
            </tr></thead>
            <tbody>
              {pg.visibles.map(p => (
                <tr key={p.id} className="border-b hover:bg-gray-50 align-top">
                  <td className="py-2 px-3"><div className="font-medium">{p.organizacionNombre}</div><div className="text-xs text-gray-500">{p.planNombre || ''} · registrado {fecha(p.createdAt)}</div></td>
                  <td className="py-2 px-3 text-right font-semibold whitespace-nowrap">{money(p.monto, p.moneda)}</td>
                  <td className="py-2 px-3 text-xs text-gray-600">
                    <div className="capitalize">{(p.metodo || '—').replace('_', ' ')}{p.banco ? ` · ${p.banco}` : ''}{p.ultimosDigitos ? ` · ****${p.ultimosDigitos}` : ''}</div>
                    <div>Ref: <span className="font-mono">{p.referencia || '—'}</span> · {fecha(p.fechaTransaccion)}</div>
                  </td>
                  <td className="py-2 px-3">
                    {urlSoporte(p.comprobanteUrl)
                      ? <a href={urlSoporte(p.comprobanteUrl)} target="_blank" rel="noopener noreferrer" className="text-primary-600 text-sm inline-flex items-center gap-1">Ver <FaExternalLinkAlt size={10} /></a>
                      : <span className="text-xs text-amber-600">Sin soporte</span>}
                  </td>
                  <td className="py-2 px-3">
                    <Badge mapa={ESTADO_PAGO} valor={p.estado} />
                    {p.motivoRechazo && <div className="text-xs text-red-600 mt-1 max-w-[14rem]">{p.motivoRechazo}</div>}
                  </td>
                  <td className="py-2 px-3 text-right whitespace-nowrap">
                    {p.estado === 'pendiente' ? (<>
                      <button onClick={() => aprobar(p)} disabled={procesando === p.id}
                        className="text-green-700 text-sm inline-flex items-center gap-1 mr-3 disabled:opacity-50"><FaCheck size={11} /> Aprobar</button>
                      <button onClick={() => setRechazar(p)} className="text-red-600 text-sm inline-flex items-center gap-1"><FaX size={11} /> Rechazar</button>
                    </>) : <span className="text-xs text-gray-400">{fecha(p.fechaValidacion)}</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="py-8 text-center text-gray-500">{estado === 'pendiente' ? 'No hay pagos esperando validación.' : 'No hay pagos con ese estado.'}</div>}
        <Paginacion {...pg.paginador} />
      </div>

      {rechazar && (
        <MotivoModal titulo="Rechazar pago" peligro textoBoton="Rechazar pago"
          descripcion={`${rechazar.organizacionNombre} · ${money(rechazar.monto, rechazar.moneda)}. La organización verá este motivo en su historial de pagos.`}
          onClose={() => setRechazar(null)}
          onConfirm={async (motivo) => { await admin.rechazarPago(rechazar.id, motivo); setRechazar(null); cargar(); }} />
      )}
    </div>
  );
}
