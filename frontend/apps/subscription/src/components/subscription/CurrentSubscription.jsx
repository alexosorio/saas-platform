import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaCrown, FaCheckCircle, FaTimesCircle, FaPlay, FaBan, FaHistory } from 'react-icons/fa';
import subscriptionService from '../../services/subscriptionService';

const estadoConfig = {
  activa:        { color: 'bg-green-100 text-green-800',   icon: <FaCheckCircle />, label: 'Activa' },
  periodo_prueba:{ color: 'bg-blue-100 text-blue-800',     icon: <FaCheckCircle />, label: 'Periodo de prueba' },
  vencida:       { color: 'bg-yellow-100 text-yellow-800', icon: <FaTimesCircle />, label: 'Vencida' },
  cancelada:     { color: 'bg-red-100 text-red-800',       icon: <FaBan />,         label: 'Cancelada' },
  suspendida:    { color: 'bg-orange-100 text-orange-800', icon: <FaBan />,         label: 'Suspendida' },
};

const CurrentSubscription = () => {
  const [sub, setSub] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { load(); }, []);

  const load = async () => {
    try {
      setLoading(true);
      const s = await subscriptionService.getCurrent();
      setSub(s);
      try { setHistory(await subscriptionService.getHistory(s.id)); } catch { setHistory([]); }
    } catch { setSub(null); }
    finally { setLoading(false); }
  };

  const doAction = async (fn, ...args) => {
    setError('');
    try { await fn(...args); load(); }
    catch (err) { setError(err.response?.data?.message || 'Error'); }
  };

  const cancel = () => {
    const motivo = window.prompt('Motivo de cancelacion:');
    if (motivo !== null) doAction(subscriptionService.cancel, motivo || 'Sin motivo');
  };

  const reactivate = () => { if (window.confirm('¿Reactivar suscripcion?')) doAction(subscriptionService.reactivate); };

  const toggleAutoRenewal = () => doAction(subscriptionService.setAutoRenewal, !sub.autoRenovacion);

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;

  if (!sub) {
    return (
      <div className="text-center py-16">
        <FaCrown className="text-yellow-500 mx-auto mb-4" size={48} />
        <h2 className="text-2xl font-bold mb-2">Aun no tienes suscripcion</h2>
        <p className="text-gray-600 mb-6">Elige un plan para comenzar</p>
        <Link to="/subscription/plans" className="bg-primary-700 hover:bg-primary-800 text-white font-medium py-3 px-6 rounded-lg">
          Ver planes
        </Link>
      </div>
    );
  }

  const cfg = estadoConfig[sub.estado] || estadoConfig.activa;
  const canCancel = sub.estado === 'activa' || sub.estado === 'periodo_prueba';
  const canReactivate = sub.estado === 'cancelada' || sub.estado === 'suspendida';

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 flex items-center gap-3"><FaCrown className="text-yellow-500" /> Mi Suscripcion</h1>

      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <div className="card mb-6">
        <div className="flex justify-between items-start mb-6">
          <div>
            <p className="text-gray-500 text-sm">Plan actual</p>
            <h2 className="text-2xl font-bold">{sub.planNombre}</h2>
          </div>
          <span className={`px-4 py-2 rounded-full font-semibold flex items-center gap-2 ${cfg.color}`}>
            {cfg.icon} {cfg.label}
          </span>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div><p className="text-gray-500 text-sm">Periodo</p><p className="font-semibold">{sub.periodo}</p></div>
          <div><p className="text-gray-500 text-sm">Inicio</p><p className="font-semibold">{sub.fechaInicio}</p></div>
          <div><p className="text-gray-500 text-sm">Fin</p><p className="font-semibold">{sub.fechaFin || '-'}</p></div>
          <div><p className="text-gray-500 text-sm">Auto-renovacion</p>
            <button onClick={toggleAutoRenewal} className={`font-semibold ${sub.autoRenovacion ? 'text-green-600' : 'text-gray-400'}`}>
              {sub.autoRenovacion ? 'Activada' : 'Desactivada'} (toggle)
            </button>
          </div>
        </div>

        <div className="flex gap-3 flex-wrap pt-4 border-t">
          <Link to="/subscription/plans" className="bg-primary-700 hover:bg-primary-800 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2">
            <FaCrown /> Cambiar de plan
          </Link>
          {canCancel && (
            <button onClick={cancel} className="bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2">
              <FaBan /> Cancelar
            </button>
          )}
          {canReactivate && (
            <button onClick={reactivate} className="bg-green-600 hover:bg-green-700 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2">
              <FaPlay /> Reactivar
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <h2 className="text-xl font-bold mb-4 flex items-center gap-2"><FaHistory /> Historial</h2>
        {history.length === 0 ? <p className="text-gray-500">Sin movimientos</p> : (
          <div className="space-y-3">
            {history.map(h => (
              <div key={h.id} className="border-l-4 border-primary-400 pl-4 py-2">
                <div className="flex justify-between items-center">
                  <span className="font-semibold capitalize">{h.accion}</span>
                  <span className="text-sm text-gray-500">{h.createdAt ? new Date(h.createdAt).toLocaleDateString() : ''}</span>
                </div>
                <div className="text-sm text-gray-600">
                  {h.planAnteriorNombre && h.planNuevoNombre && h.planAnteriorNombre !== h.planNuevoNombre
                    ? `${h.planAnteriorNombre} → ${h.planNuevoNombre}`
                    : h.planNuevoNombre}
                </div>
                {h.motivo && <div className="text-xs text-gray-500 italic">"{h.motivo}"</div>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default CurrentSubscription;
