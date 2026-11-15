import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionLocal } from '@saas/ui-shell';
import { FaCrown, FaSearch, FaPlay, FaPause, FaSyncAlt } from 'react-icons/fa';
import admin from '../../services/adminSubscriptionService';
import { AdminTabs, Badge, ESTADO_SUSCRIPCION, MotivoModal, fecha } from './adminUi';

const vence = (s) => {
  if (s.diasParaVencer == null) return <span className="text-gray-400">—</span>;
  if (s.diasParaVencer < 0) return <span className="text-red-600">{fecha(s.fechaFin)} <span className="text-xs">(hace {-s.diasParaVencer} d)</span></span>;
  if (s.diasParaVencer <= 5) return <span className="text-amber-600">{fecha(s.fechaFin)} <span className="text-xs">(en {s.diasParaVencer} d)</span></span>;
  return <span>{fecha(s.fechaFin)}</span>;
};

export default function AdminSuscripciones() {
  const [items, setItems] = useState([]);
  const [estado, setEstado] = useState('');
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [accion, setAccion] = useState(null);   // { tipo: 'activar'|'suspender', s }

  const cargar = async () => {
    setLoading(true); setError('');
    try { setItems(await admin.suscripciones({ estado, q: busqueda })); }
    catch (e) { setError(e.response?.status === 403 ? 'No tienes permiso de administración de la plataforma.' : (e.response?.data?.message || 'Error al cargar')); }
    finally { setLoading(false); }
  };
  useEffect(() => { cargar(); /* eslint-disable-next-line */ }, [estado, busqueda]);
  useEffect(() => { const t = setTimeout(() => setBusqueda(q), 350); return () => clearTimeout(t); }, [q]);

  const ejecutarVencimientos = async () => {
    if (!window.confirm('¿Ejecutar ahora la suspensión de suscripciones vencidas sin pago?')) return;
    try {
      const r = await admin.ejecutarVencimientos();
      setAviso(r.suspendidas ? `${r.suspendidas} suscripción(es) suspendida(s) por falta de pago.` : 'No hay suscripciones vencidas por suspender.');
      cargar();
    } catch (e) { setError(e.response?.data?.message || 'No se pudo ejecutar el proceso'); }
  };

  // Paginación en el navegador (la lista ya está completa en memoria)
  const pg = usePaginacionLocal(items, { clave: 'admin-suscripciones', reiniciar: `${estado}|${busqueda}` });
  const conteo = (e) => items.filter(s => s.estado === e).length;

  return (
    <div>
      <div className="flex flex-wrap justify-between items-center gap-3 mb-4">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaCrown className="text-primary-600" /> Administración de suscripciones</h1>
        <button onClick={ejecutarVencimientos} className="btn-secondary flex items-center gap-2 text-sm" title="Suspende las suscripciones vencidas (también corre cada día automáticamente)">
          <FaSyncAlt /> Procesar vencimientos ahora
        </button>
      </div>
      <AdminTabs />
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      {aviso && <div className="bg-primary-50 text-primary-800 px-4 py-3 rounded mb-4">{aviso}</div>}

      <div className="card mb-4 flex flex-wrap items-center gap-2">
        {['', 'activa', 'periodo_prueba', 'pendiente_pago', 'suspendida', 'vencida', 'cancelada'].map(e => (
          <button key={e} onClick={() => setEstado(e)}
            className={`px-3 py-1 rounded text-sm ${estado === e ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700'}`}>
            {e === '' ? 'Todas' : ESTADO_SUSCRIPCION[e][0]}{e && !estado ? ` (${conteo(e)})` : ''}
          </button>
        ))}
        <div className="relative ml-auto w-full md:w-72">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input className="input-field pl-9" placeholder="Buscar organización o correo…" value={q} onChange={e => setQ(e.target.value)} />
        </div>
      </div>

      <div className="card overflow-x-auto">
        {loading ? <div className="py-8 text-center text-gray-500">Cargando…</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b">
              <th className="py-2 px-3">Organización</th><th className="py-2 px-3">Plan</th><th className="py-2 px-3">Estado</th>
              <th className="py-2 px-3">Vence</th><th className="py-2 px-3 text-center">Pagos por validar</th><th className="py-2 px-3 text-right">Acciones</th>
            </tr></thead>
            <tbody>
              {pg.visibles.map(s => (
                <tr key={s.id} className="border-b hover:bg-gray-50">
                  <td className="py-2 px-3"><div className="font-medium">{s.organizacionNombre}</div><div className="text-xs text-gray-500">{s.organizacionEmail || ''}</div></td>
                  <td className="py-2 px-3">{s.planNombre || '—'} <span className="text-xs text-gray-400">{s.periodo}</span></td>
                  <td className="py-2 px-3"><Badge mapa={ESTADO_SUSCRIPCION} valor={s.estado} /></td>
                  <td className="py-2 px-3 whitespace-nowrap">{vence(s)}</td>
                  <td className="py-2 px-3 text-center">{s.pagosPendientes > 0
                    ? <span className="text-xs font-semibold bg-amber-100 text-amber-700 px-2 py-0.5 rounded">{s.pagosPendientes}</span>
                    : <span className="text-gray-300">0</span>}</td>
                  <td className="py-2 px-3 text-right whitespace-nowrap">
                    {s.estado !== 'activa' && (
                      <button onClick={() => setAccion({ tipo: 'activar', s })} className="text-green-700 text-sm inline-flex items-center gap-1 mr-3"><FaPlay size={10} /> Activar</button>
                    )}
                    {s.estado !== 'suspendida' && s.estado !== 'cancelada' && (
                      <button onClick={() => setAccion({ tipo: 'suspender', s })} className="text-red-600 text-sm inline-flex items-center gap-1"><FaPause size={10} /> Suspender</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="py-8 text-center text-gray-500">No hay suscripciones con esos filtros.</div>}
        <Paginacion {...pg.paginador} />
      </div>

      {accion && (
        <MotivoModal
          titulo={accion.tipo === 'activar' ? 'Activar suscripción' : 'Suspender suscripción'}
          descripcion={accion.tipo === 'activar'
            ? `${accion.s.organizacionNombre} podrá usar la plataforma. Si la vigencia ya venció, se extiende un período desde hoy.`
            : `${accion.s.organizacionNombre} perderá el acceso a la plataforma hasta que se reactive o registre un pago.`}
          obligatorio={accion.tipo === 'suspender'} peligro={accion.tipo === 'suspender'}
          textoBoton={accion.tipo === 'activar' ? 'Activar' : 'Suspender'}
          onClose={() => setAccion(null)}
          onConfirm={async (motivo) => {
            if (accion.tipo === 'activar') await admin.activar(accion.s.id, motivo);
            else await admin.suspender(accion.s.id, motivo);
            setAccion(null); setAviso(''); cargar();
          }} />
      )}
    </div>
  );
}
