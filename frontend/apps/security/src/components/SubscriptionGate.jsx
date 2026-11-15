import { useState, useEffect, useCallback } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { FaLock, FaCreditCard, FaExclamationTriangle, FaTimes, FaMoneyBillWave } from 'react-icons/fa';

const SUB_API = import.meta.env.VITE_SUBSCRIPTION_API_URL || 'http://localhost:8084';
const ACTIVOS = ['activa', 'periodo_prueba'];
const fecha = (iso) => {
  if (!iso) return '';
  const [y, m, d] = String(iso).split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
};
const hoyISO = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };

// Bloquea el uso de la plataforma si no hay suscripción activa/pagada (o si ya pasó su fecha de fin).
// Permite solo la sección /subscription (para elegir plan y registrar el pago).
// Antes del vencimiento muestra un aviso con los días que faltan.
export default function SubscriptionGate({ children }) {
  const [estado, setEstado] = useState({ loading: true, activa: false, valor: null, fechaFin: null, dias: null, diasAviso: 7 });
  const [ocultarAviso, setOcultarAviso] = useState(() => {
    try { return sessionStorage.getItem('aviso-vencimiento-oculto') === hoyISO(); } catch { return false; }
  });
  const location = useLocation();

  const check = useCallback(() => {
    fetch(`${SUB_API}/api/subscription/current`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}` },
    })
      .then(async (r) => {
        if (!r.ok) return setEstado((e) => ({ ...e, loading: false, activa: false, valor: null }));
        const d = await r.json().catch(() => ({}));
        // Además del estado, la fecha: si ya pasó, se bloquea aunque el servidor no la haya marcado aún
        const vencidaPorFecha = d?.fechaFin && d.fechaFin < hoyISO();
        setEstado({
          loading: false,
          activa: ACTIVOS.includes(d?.estado) && !vencidaPorFecha,
          valor: vencidaPorFecha && ACTIVOS.includes(d?.estado) ? 'vencida' : (d?.estado || null),
          fechaFin: d?.fechaFin || null,
          dias: typeof d?.diasParaVencer === 'number' ? d.diasParaVencer : null,
          diasAviso: typeof d?.diasAviso === 'number' ? d.diasAviso : 7,
        });
      })
      .catch(() => setEstado((e) => ({ ...e, loading: false, activa: false, valor: null })));
  }, []);

  useEffect(() => { check(); }, [location.pathname, check]);
  // Con la app abierta: revisa cada 10 minutos y cuando el catálogo responde "suscripción inactiva"
  useEffect(() => {
    const t = setInterval(check, 10 * 60 * 1000);
    window.addEventListener('suscripcion-inactiva', check);
    return () => { clearInterval(t); window.removeEventListener('suscripcion-inactiva', check); };
  }, [check]);

  if (estado.loading)
    return <div className="flex justify-center items-center py-20 text-gray-500">Verificando suscripción…</div>;

  // La súper organización (administración de la plataforma) no requiere suscripción
  const esPlataforma = (() => {
    try {
      let b = (localStorage.getItem('accessToken') || '').split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      while (b.length % 4) b += '=';
      return (JSON.parse(atob(b)).permisos || []).some(x => x.startsWith('plataforma_'));
    } catch { return false; }
  })();

  if (esPlataforma) return children;

  if (estado.activa || location.pathname.startsWith('/subscription')) {
    const avisar = estado.activa && estado.dias != null && estado.dias >= 0 && estado.dias <= estado.diasAviso && !ocultarAviso;
    const urgente = estado.dias != null && estado.dias <= 1;
    const cuando = estado.dias === 0 ? 'vence hoy' : estado.dias === 1 ? 'vence mañana' : `vence en ${estado.dias} días`;
    return (
      <>
        {avisar && (
          <div className={`mb-4 rounded-lg border px-4 py-3 text-sm flex flex-wrap items-center gap-3
            ${urgente ? 'border-red-200 bg-red-50 text-red-800' : 'border-amber-200 bg-amber-50 text-amber-900'}`}>
            <FaExclamationTriangle className={`shrink-0 ${urgente ? 'text-red-500' : 'text-amber-500'}`} />
            <span className="flex-1 min-w-[240px]">
              Tu suscripción <strong>{cuando}</strong> ({fecha(estado.fechaFin)}). Si vence sin pago, el acceso se bloquea automáticamente.
            </span>
            <Link to="/subscription/payments" className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 font-medium text-white
              ${urgente ? 'bg-red-600 hover:bg-red-700' : 'bg-primary-600 hover:bg-primary-700'}`}>
              <FaMoneyBillWave /> Pagar ahora
            </Link>
            {!urgente && (
              <button type="button" title="Ocultar por hoy" className="opacity-70 hover:opacity-100"
                onClick={() => { setOcultarAviso(true); try { sessionStorage.setItem('aviso-vencimiento-oculto', hoyISO()); } catch { /* */ } }}>
                <FaTimes />
              </button>
            )}
          </div>
        )}
        {children}
      </>
    );
  }

  const motivo = estado.valor === 'vencida' ? `Tu suscripción venció${estado.fechaFin ? ' el ' + fecha(estado.fechaFin) : ''}.`
    : estado.valor === 'suspendida' ? `Tu suscripción está suspendida por falta de pago${estado.fechaFin ? ' (venció el ' + fecha(estado.fechaFin) + ')' : ''}.`
    : estado.valor === 'cancelada' ? 'Tu suscripción fue cancelada.'
    : 'Aún no tienes una suscripción activa.';

  return (
    <div className="max-w-xl mx-auto mt-10 text-center">
      <div className="card">
        <div className="w-14 h-14 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center mx-auto mb-4">
          <FaLock size={22} />
        </div>
        <h1 className="text-xl font-bold mb-2">Activa tu suscripción para continuar</h1>
        <p className="text-gray-600 mb-1">{motivo}</p>
        <p className="text-gray-500 text-sm mb-5">
          El acceso se habilita de nuevo apenas se registre y valide el pago de tu plan. Tus datos se conservan.
        </p>
        <div className="flex flex-col sm:flex-row gap-2 justify-center">
          <Link to="/subscription/payments" className="btn-primary inline-flex items-center justify-center gap-2">
            <FaMoneyBillWave /> Registrar pago
          </Link>
          <Link to="/subscription/plans" className="btn-secondary inline-flex items-center justify-center gap-2">
            <FaCreditCard /> Ver planes
          </Link>
        </div>
      </div>
    </div>
  );
}
