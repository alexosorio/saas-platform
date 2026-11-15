import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaBuilding, FaUser, FaCrown, FaCheck, FaArrowLeft, FaArrowRight } from 'react-icons/fa';
import LogoUploader from '../common/LogoUploader';

const USER_API = import.meta.env.VITE_API_URL || 'http://localhost:8082';
const SUB_API = import.meta.env.VITE_SUBSCRIPTION_API_URL || 'http://localhost:8084';
const money = (n) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(Number(n || 0));
const PASOS = [['Organización', FaBuilding], ['Tu cuenta', FaUser], ['Plan', FaCrown], ['Confirmar', FaCheck]];

export default function RegistroWizard() {
  const [paso, setPaso] = useState(0);
  const [f, setF] = useState({ organizacion: '', telefono: '', nombre: '', apellido: '', email: '', password: '', confirmar: '',
                                planId: null, periodo: 'mensual', aceptaTerminos: false, logo: null });
  const [planes, setPlanes] = useState([]);
  const [diasPrueba, setDiasPrueba] = useState(null);
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const set = (c) => setF((x) => ({ ...x, ...c }));

  useEffect(() => {
    fetch(`${SUB_API}/api/public/plans`).then(r => r.ok ? r.json() : []).then(setPlanes).catch(() => setPlanes([]));
    fetch(`${USER_API}/api/public/config`).then(r => r.ok ? r.json() : {}).then(d => setDiasPrueba(d.diasPrueba ?? null)).catch(() => {});
  }, []);

  const validarPaso = () => {
    if (paso === 0 && !f.organizacion.trim()) return 'Escribe el nombre de tu organización';
    if (paso === 1) {
      if (!f.nombre.trim()) return 'Escribe tu nombre';
      if (!/^\S+@\S+\.\S+$/.test(f.email)) return 'Escribe un correo válido';
      if (f.password.length < 8) return 'La contraseña debe tener al menos 8 caracteres';
      if (f.password !== f.confirmar) return 'Las contraseñas no coinciden';
    }
    if (paso === 2 && !f.planId) return 'Elige un plan';
    if (paso === 3 && !f.aceptaTerminos) return 'Debes aceptar los términos y la política de datos';
    return '';
  };
  const siguiente = () => { const e = validarPaso(); setError(e); if (!e) setPaso(p => p + 1); };

  const finalizar = async () => {
    const e = validarPaso(); if (e) { setError(e); return; }
    setEnviando(true); setError('');
    try {
      // 1) Organización + usuario propietario (deja la sesión iniciada)
      const r = await fetch(`${USER_API}/api/auth/registro-organizacion`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ organizacion: f.organizacion, telefono: f.telefono, nombre: f.nombre, apellido: f.apellido,
                               email: f.email, password: f.password, aceptaTerminos: f.aceptaTerminos,
                               logo: f.logo || null }),
      });
      const auth = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(auth.message || 'No se pudo completar el registro');
      localStorage.setItem('accessToken', auth.accessToken);
      localStorage.setItem('refreshToken', auth.refreshToken);
      localStorage.setItem('user', JSON.stringify(auth));

      // 2) Prueba gratuita del plan elegido
      const s = await fetch(`${SUB_API}/api/subscription/trial`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${auth.accessToken}` },
        body: JSON.stringify({ planId: f.planId, periodo: f.periodo }),
      });
      if (!s.ok) {
        const d = await s.json().catch(() => ({}));
        // La cuenta ya existe: entra igual y podrá elegir plan desde Suscripción
        window.location.href = `/subscription/plans?aviso=${encodeURIComponent(d.message || 'No se pudo activar la prueba gratuita')}`;
        return;
      }
      window.location.href = '/dashboard';
    } catch (err) { setError(err.message); setEnviando(false); }
  };

  const plan = planes.find(p => p.id === f.planId);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-primary-100 p-4">
      <div className="bg-white rounded-xl shadow-lg w-full max-w-2xl p-6 md:p-8">
        <h1 className="text-2xl font-bold text-center mb-1">Crea tu cuenta</h1>
        <p className="text-center text-gray-500 text-sm mb-6">
          {diasPrueba > 0 ? `Empieza con ${diasPrueba} días de prueba gratis, sin tarjeta.` : 'Registra tu organización en pocos pasos.'}
        </p>

        {/* Indicador de pasos */}
        <ol className="flex items-center justify-between mb-8">
          {PASOS.map(([label, Icon], i) => (
            <li key={label} className="flex-1 flex flex-col items-center relative">
              {i > 0 && <span className={`absolute top-4 right-1/2 w-full h-0.5 ${i <= paso ? 'bg-primary-600' : 'bg-gray-200'}`} />}
              <span className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center text-sm
                ${i < paso ? 'bg-primary-600 text-white' : i === paso ? 'bg-primary-600 text-white ring-4 ring-primary-100' : 'bg-gray-200 text-gray-500'}`}>
                {i < paso ? <FaCheck size={12} /> : <Icon size={13} />}
              </span>
              <span className={`text-xs mt-1 ${i === paso ? 'text-primary-700 font-medium' : 'text-gray-500'}`}>{label}</span>
            </li>
          ))}
        </ol>

        {error && <div className="bg-red-100 text-red-700 px-4 py-2 rounded mb-4 text-sm">{error}</div>}

        {paso === 0 && (
          <div className="space-y-4">
            <div><label className="block font-semibold mb-1">Nombre de la organización *</label>
              <input className="input-field" value={f.organizacion} onChange={e => set({ organizacion: e.target.value })} placeholder="Ej: Celulares del Caribe" autoFocus /></div>
            <div><label className="block font-semibold mb-1">Teléfono</label>
              <input className="input-field" value={f.telefono} onChange={e => set({ telefono: e.target.value })} placeholder="Opcional" /></div>
            <div><label className="block font-semibold mb-1">Logo <span className="font-normal text-gray-500 text-sm">(opcional)</span></label>
              <LogoUploader value={f.logo} onChange={(logo) => set({ logo })}
                ayuda="Aparecerá en tus correos, en el menú y en las listas de precios que compartas. Puedes agregarlo o cambiarlo después en Configuración." />
            </div>
          </div>
        )}

        {paso === 1 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div><label className="block font-semibold mb-1">Nombre *</label><input className="input-field" value={f.nombre} onChange={e => set({ nombre: e.target.value })} autoFocus /></div>
            <div><label className="block font-semibold mb-1">Apellido</label><input className="input-field" value={f.apellido} onChange={e => set({ apellido: e.target.value })} /></div>
            <div className="md:col-span-2"><label className="block font-semibold mb-1">Correo *</label><input type="email" className="input-field" value={f.email} onChange={e => set({ email: e.target.value })} /></div>
            <div><label className="block font-semibold mb-1">Contraseña *</label><input type="password" className="input-field" value={f.password} onChange={e => set({ password: e.target.value })} placeholder="Mínimo 8 caracteres" /></div>
            <div><label className="block font-semibold mb-1">Confirmar contraseña *</label><input type="password" className="input-field" value={f.confirmar} onChange={e => set({ confirmar: e.target.value })} /></div>
            <p className="md:col-span-2 text-xs text-gray-500">Serás el <strong>administrador</strong> de la organización y podrás invitar a tu equipo después. Te enviaremos un enlace para confirmar tu correo: hasta confirmarlo no podrás hacer operaciones.</p>
          </div>
        )}

        {paso === 2 && (
          <div>
            <div className="flex justify-center gap-2 mb-4">
              {['mensual', 'anual'].map(p => (
                <button key={p} type="button" onClick={() => set({ periodo: p })}
                  className={`px-4 py-1.5 rounded-full text-sm ${f.periodo === p ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700'}`}>
                  {p === 'mensual' ? 'Mensual' : 'Anual'}
                </button>
              ))}
            </div>
            {planes.length === 0 && <p className="text-center text-gray-500 text-sm py-6">No hay planes disponibles en este momento.</p>}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {planes.map(p => (
                <button key={p.id} type="button" onClick={() => set({ planId: p.id })}
                  className={`text-left border-2 rounded-lg p-4 transition ${f.planId === p.id ? 'border-primary-600 bg-primary-50' : 'border-gray-200 hover:border-primary-300'}`}>
                  <div className="font-bold">{p.nombre}</div>
                  <div className="text-xl font-bold text-primary-700 mt-1">
                    {money(f.periodo === 'anual' ? p.precioAnual : p.precioMensual)}
                    <span className="text-xs font-normal text-gray-500">/{f.periodo === 'anual' ? 'año' : 'mes'}</span>
                  </div>
                  {p.descripcion && <p className="text-xs text-gray-600 mt-2">{p.descripcion}</p>}
                  {p.maxUsuarios && <p className="text-xs text-gray-500 mt-1">Hasta {p.maxUsuarios} usuario(s)</p>}
                </button>
              ))}
            </div>
            {diasPrueba > 0 && <p className="text-center text-sm text-gray-600 mt-4">No pagas nada durante los primeros <strong>{diasPrueba} días</strong>.</p>}
          </div>
        )}

        {paso === 3 && (
          <div className="space-y-4">
            <div className="bg-gray-50 rounded-lg p-4 text-sm space-y-1">
              <div className="flex items-center gap-3"><span className="text-gray-500">Organización:</span> <strong>{f.organizacion}</strong>
                {f.logo && <img src={f.logo} alt="Logo" className="h-8 w-auto max-w-[120px] object-contain" />}</div>
              <div><span className="text-gray-500">Administrador:</span> {f.nombre} {f.apellido} · {f.email}</div>
              <div><span className="text-gray-500">Plan:</span> {plan?.nombre} ({f.periodo})
                {diasPrueba > 0 && <span className="text-primary-700"> · {diasPrueba} días de prueba gratis</span>}</div>
            </div>
            <label className="flex items-start gap-2 text-sm cursor-pointer">
              <input type="checkbox" className="mt-1" checked={f.aceptaTerminos} onChange={e => set({ aceptaTerminos: e.target.checked })} />
              <span>Acepto los términos del servicio y autorizo el tratamiento de mis datos según la
                {' '}<a href="/legal/politica-datos" target="_blank" rel="noopener noreferrer" className="text-primary-700 underline">política de datos</a> (Ley 1581 de 2012).</span>
            </label>
          </div>
        )}

        <div className="flex justify-between items-center mt-8">
          {paso > 0
            ? <button type="button" onClick={() => { setError(''); setPaso(p => p - 1); }} className="btn-secondary flex items-center gap-2"><FaArrowLeft /> Atrás</button>
            : <Link to="/login" className="text-sm text-gray-500 hover:text-gray-700">Ya tengo cuenta</Link>}
          {paso < 3
            ? <button type="button" onClick={siguiente} className="btn-primary flex items-center gap-2">Siguiente <FaArrowRight /></button>
            : <button type="button" onClick={finalizar} disabled={enviando} className="btn-primary flex items-center gap-2 disabled:opacity-50">
                {enviando ? 'Creando tu cuenta…' : 'Crear cuenta'} <FaCheck /></button>}
        </div>
      </div>
    </div>
  );
}
