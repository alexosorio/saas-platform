import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaCheck, FaCrown, FaUsers, FaDatabase } from 'react-icons/fa';
import planService from '../../services/planService';
import subscriptionService from '../../services/subscriptionService';

const PlanList = () => {
  const navigate = useNavigate();
  const [plans, setPlans] = useState([]);
  const [current, setCurrent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [periodo, setPeriodo] = useState('mensual');
  const [subscribing, setSubscribing] = useState(null);

  useEffect(() => { load(); }, []);

  const load = async () => {
    try {
      setLoading(true);
      const ps = await planService.getActive();
      setPlans(ps);
      try { setCurrent(await subscriptionService.getCurrent()); } catch { setCurrent(null); }
    } catch (e) { setError('Error al cargar planes'); }
    finally { setLoading(false); }
  };

  const subscribe = async (planId) => {
    const motivo = window.prompt('Motivo del cambio (opcional):') || '';
    setSubscribing(planId); setError('');
    try {
      await subscriptionService.subscribe({ planId, periodo, autoRenovacion: true, motivo });
      navigate('/subscription');
    } catch (err) { setError(err.response?.data?.message || 'Error al suscribir'); }
    finally { setSubscribing(null); }
  };

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando planes...</div>;

  return (
    <div>
      <div className="text-center mb-8">
        <h1 className="text-4xl font-bold flex items-center justify-center gap-3"><FaCrown className="text-yellow-500" /> Planes disponibles</h1>
        <p className="text-gray-600 mt-2">Elige el plan que mejor se adapte a tu organizacion</p>
      </div>

      <div className="flex justify-center gap-2 mb-8">
        <button onClick={() => setPeriodo('mensual')}
          className={`px-6 py-2 rounded-l-full ${periodo === 'mensual' ? 'bg-primary-700 text-white' : 'bg-gray-200 text-gray-700'}`}>
          Mensual
        </button>
        <button onClick={() => setPeriodo('anual')}
          className={`px-6 py-2 rounded-r-full ${periodo === 'anual' ? 'bg-primary-700 text-white' : 'bg-gray-200 text-gray-700'}`}>
          Anual <span className="text-xs ml-1 text-green-600">(ahorra)</span>
        </button>
      </div>

      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4 max-w-2xl mx-auto">{error}</div>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-6xl mx-auto">
        {plans.map(p => {
          const esActual = current && current.planId === p.id && current.estado === 'activa';
          const precio = periodo === 'anual' ? (p.precioAnual || (p.precioMensual * 12)) : p.precioMensual;
          return (
            <div key={p.id} className={`card border-2 ${esActual ? 'border-primary-500 ring-4 ring-primary-100' : 'border-gray-200'}`}>
              <h3 className="text-2xl font-bold">{p.nombre}</h3>
              <p className="text-gray-600 mt-1 mb-4 text-sm">{p.descripcion}</p>
              <div className="my-6">
                <span className="text-4xl font-bold">${(precio || 0).toLocaleString()}</span>
                <span className="text-gray-500"> /{periodo === 'anual' ? 'año' : 'mes'}</span>
              </div>
              <ul className="space-y-2 text-sm mb-6">
                <li className="flex items-center gap-2"><FaUsers className="text-primary-600" />
                  <span>{p.maxUsuarios === -1 ? 'Usuarios ilimitados' : `${p.maxUsuarios} usuarios`}</span></li>
                <li className="flex items-center gap-2"><FaDatabase className="text-primary-600" />
                  <span>{p.almacenamientoGb} GB de almacenamiento</span></li>
                {p.modulos && p.modulos.map(m => (
                  <li key={m.id} className="flex items-center gap-2"><FaCheck className="text-green-600" /> <span>{m.nombre}</span></li>
                ))}
              </ul>
              {esActual ? (
                <div className="text-center py-2 bg-primary-100 text-primary-800 rounded font-semibold">Plan actual</div>
              ) : (
                <button onClick={() => subscribe(p.id)} disabled={subscribing === p.id}
                  className="w-full bg-primary-700 hover:bg-primary-800 text-white font-medium py-2 px-4 rounded-lg">
                  {subscribing === p.id ? 'Procesando...' : current ? 'Cambiar a este plan' : 'Suscribirme'}
                </button>
              )}
            </div>
          );
        })}
      </div>
      {plans.length === 0 && <div className="text-center py-10 text-gray-500">No hay planes disponibles</div>}
    </div>
  );
};

export default PlanList;
