import { useState, useEffect } from 'react';
import { FaCog, FaUndo, FaSave } from 'react-icons/fa';
import parametroService from '../../services/parametroService';
import MarcaOrganizacion from './MarcaOrganizacion';

export default function Configuracion() {
  const [items, setItems] = useState([]);
  const [valores, setValores] = useState({});
  const [general, setGeneral] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [guardando, setGuardando] = useState(null);
  // La plataforma ve los parámetros de alcance 'plataforma'; si hay alguno, es administrador de plataforma
  const esPlataforma = items.some(p => p.alcance === 'plataforma');

  const cargar = async () => {
    setLoading(true);
    try {
      const l = await parametroService.listar();
      setItems(l);
      setValores(Object.fromEntries(l.map(p => [p.clave, p.valorEfectivo])));
      setGeneral(Object.fromEntries(l.map(p => [p.clave, p.alcance === 'plataforma'])));
    } catch (e) { setError(e.response?.status === 403 ? 'No tienes permiso para ver la configuración.' : 'No se pudo cargar la configuración'); }
    finally { setLoading(false); }
  };
  useEffect(() => { cargar(); }, []);

  const guardar = async (p) => {
    setGuardando(p.clave); setError(''); setOk('');
    try {
      await parametroService.actualizar(p.clave, valores[p.clave], !!general[p.clave]);
      setOk(`"${p.nombre}" actualizado.`); await cargar();
    } catch (e) { setError(e.response?.data?.message || 'No se pudo guardar'); }
    finally { setGuardando(null); }
  };
  const restablecer = async (p) => {
    setGuardando(p.clave); setError(''); setOk('');
    try { await parametroService.restablecer(p.clave); setOk(`"${p.nombre}" vuelve al valor general.`); await cargar(); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo restablecer'); }
    finally { setGuardando(null); }
  };

  const categorias = [...new Set(items.map(p => p.categoria || 'General'))];

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando…</div>;
  return (
    <div className="max-w-4xl">
      <h1 className="text-3xl font-bold flex items-center gap-3 mb-2"><FaCog className="text-primary-600" /> Configuración</h1>
      <p className="text-gray-500 text-sm mb-5">
        Ajusta los valores de tu organización. Si no defines uno propio, se usa el valor general de la plataforma.
      </p>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      {ok && <div className="bg-green-100 text-green-700 px-4 py-3 rounded mb-4">{ok}</div>}

      <MarcaOrganizacion />

      {categorias.map(cat => (
        <div key={cat} className="card mb-4">
          <h2 className="font-semibold text-primary-700 mb-3">{cat}</h2>
          <div className="divide-y">
            {items.filter(p => (p.categoria || 'General') === cat).map(p => {
              const cambiado = String(valores[p.clave]) !== String(p.valorEfectivo);
              return (
                <div key={p.clave} className="py-3 flex flex-col md:flex-row md:items-center gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="font-medium">{p.nombre}
                      {p.alcance === 'plataforma' && <span className="ml-2 text-[11px] bg-primary-100 text-primary-700 px-1.5 py-0.5 rounded">Plataforma</span>}
                    </div>
                    <div className="text-xs text-gray-500">{p.descripcion}</div>
                    {p.alcance === 'organizacion' && (
                      <div className="text-xs mt-1">
                        {p.valorOrganizacion != null
                          ? <span className="text-primary-700">Valor propio de tu organización (general: {p.valorGeneral})</span>
                          : <span className="text-gray-400">Usando el valor general</span>}
                      </div>
                    )}
                  </div>
                  {/* Valor + acciones en un bloque propio: el campo nunca se monta sobre los botones */}
                  <div className="flex flex-wrap items-start gap-2 md:shrink-0 md:w-[29rem] md:justify-end">
                  <div className={p.tipo === 'entero' || p.tipo === 'decimal' ? 'w-full sm:w-28' : 'w-full sm:w-72'}>
                    <input type={p.tipo === 'entero' || p.tipo === 'decimal' ? 'number' : 'text'} className="input-field w-full" value={valores[p.clave] ?? ''} disabled={!p.editable}
                      title={String(valores[p.clave] ?? '')}
                      min={p.minimo ?? undefined} max={p.maximo ?? undefined}
                      onChange={e => setValores(v => ({ ...v, [p.clave]: e.target.value }))} />
                  </div>
                  <div className="flex flex-col gap-1 shrink-0 md:w-[10.5rem]">
                    <div className="flex gap-2">
                      <button onClick={() => guardar(p)} disabled={!p.editable || !cambiado || guardando === p.clave}
                        className="btn-primary text-sm flex items-center gap-1 whitespace-nowrap disabled:opacity-40"><FaSave /> Guardar</button>
                      {p.alcance === 'organizacion' && p.valorOrganizacion != null && (
                        <button onClick={() => restablecer(p)} disabled={guardando === p.clave} title="Volver al valor general"
                          className="btn-secondary text-sm flex items-center gap-1"><FaUndo /></button>
                      )}
                    </div>
                    {esPlataforma && p.alcance === 'organizacion' && (
                      <label className="text-xs text-gray-500 flex items-center gap-1">
                        <input type="checkbox" checked={!!general[p.clave]} onChange={e => setGeneral(g => ({ ...g, [p.clave]: e.target.checked }))} />
                        Guardar como valor general
                      </label>
                    )}
                  </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
