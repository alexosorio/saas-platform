import { useState, useEffect, useCallback } from 'react';
import { FaEnvelopeOpenText, FaPaperPlane, FaKey, FaSyncAlt } from 'react-icons/fa';
import userService from '../services/userService';
import authService from '../services/authService';
import { renovarToken } from '../services/api';

// Claim 'emailVerificado' del access token actual (null si no lo trae)
const claimVerificado = () => {
  try {
    let b = (localStorage.getItem('accessToken') || '').split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b.length % 4) b += '=';
    const v = JSON.parse(atob(b)).emailVerificado;
    return typeof v === 'boolean' ? v : null;
  } catch { return null; }
};

/**
 * Mientras el correo no esté confirmado, en lugar de la aplicación se muestra esta pantalla
 * (el backend igual rechaza cualquier operación). Al confirmar, renueva el token para que
 * catálogo y suscripciones también lo reconozcan.
 */
export default function VerificacionGate({ children }) {
  const [yo, setYo] = useState(null);           // null = cargando
  const [listo, setListo] = useState(false);    // verificado y con token al día
  const [modo, setModo] = useState('inicio');   // inicio | codigo
  const [codigo, setCodigo] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [msg, setMsg] = useState({ tipo: '', texto: '' });

  const comprobar = useCallback(async (manual = false) => {
    try {
      const u = await userService.getCurrentUser();
      setYo(u);
      if (u.emailVerificado) {
        // El token puede ser anterior a la verificación: se renueva para que lo lleve
        if (claimVerificado() === false) { try { await renovarToken(); } catch { /* el interceptor maneja la sesión */ } }
        setListo(true);
      } else if (manual) {
        setMsg({ tipo: 'error', texto: 'Tu correo todavía no aparece confirmado. Abre el enlace del correo o escribe el código.' });
      }
    } catch {
      setYo((y) => y || { emailVerificado: true });   // sin respuesta: no bloquear la app por un fallo de red
      setListo(true);
    }
  }, []);

  useEffect(() => { comprobar(); }, [comprobar]);

  // Si confirma en otra pestaña (o desde el celular), al volver a esta se detecta solo
  useEffect(() => {
    if (listo) return undefined;
    const alVolver = () => { if (document.visibilityState === 'visible') comprobar(); };
    document.addEventListener('visibilitychange', alVolver);
    window.addEventListener('focus', alVolver);
    return () => { document.removeEventListener('visibilitychange', alVolver); window.removeEventListener('focus', alVolver); };
  }, [listo, comprobar]);

  // Un servicio respondió "correo sin verificar": volver a mostrar esta pantalla
  useEffect(() => {
    const h = () => { setListo(false); comprobar(); };
    window.addEventListener('correo-sin-verificar', h);
    return () => window.removeEventListener('correo-sin-verificar', h);
  }, [comprobar]);

  if (listo) return children;
  if (!yo) return <div className="text-center py-16 text-gray-500">Cargando…</div>;

  const reenviar = async () => {
    setOcupado(true); setMsg({ tipo: '', texto: '' });
    try { setMsg({ tipo: 'ok', texto: (await userService.reenviarVerificacionPropia()).message }); }
    catch (e) {
      if (e.response?.status === 409) { comprobar(); return; }
      setMsg({ tipo: 'error', texto: e.response?.data?.message || 'No se pudo enviar el enlace' });
    } finally { setOcupado(false); }
  };

  const verificarCodigo = async (e) => {
    e.preventDefault();
    setOcupado(true); setMsg({ tipo: '', texto: '' });
    try { await authService.verifyEmail(yo.email, codigo.trim()); await comprobar(); }
    catch (err) { setMsg({ tipo: 'error', texto: err.response?.data?.message || 'Código inválido o vencido' }); }
    finally { setOcupado(false); }
  };

  return (
    <div className="max-w-xl mx-auto mt-6">
      <div className="card text-center">
        <FaEnvelopeOpenText className="text-primary-600 mx-auto mb-4" size={52} />
        <h1 className="text-2xl font-bold mb-2">Confirma tu correo para empezar</h1>
        <p className="text-gray-600 mb-1">Te enviamos un enlace de verificación a</p>
        <p className="font-semibold text-gray-800 mb-4 break-all">{yo.email}</p>
        <p className="text-sm text-gray-500 mb-6">
          Hasta que lo confirmes no podrás hacer operaciones. Si no lo ves, revisa la carpeta de correo no deseado.
        </p>

        {msg.texto && (
          <div className={`px-4 py-3 rounded mb-4 text-sm text-left ${msg.tipo === 'ok' ? 'bg-green-50 border border-green-200 text-green-800' : 'bg-red-100 text-red-700'}`}>
            {msg.texto}
          </div>
        )}

        {modo === 'codigo' ? (
          <form onSubmit={verificarCodigo} className="space-y-3 text-left">
            <label className="block text-gray-700 text-sm">Código de 6 dígitos del correo</label>
            <input type="text" inputMode="numeric" maxLength={6} pattern="\d{6}" required autoFocus
              className="input-field text-center font-mono text-2xl tracking-[0.5em]"
              value={codigo} onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="••••••" />
            <div className="flex gap-2">
              <button type="submit" disabled={ocupado} className="btn-primary flex-1">{ocupado ? 'Verificando…' : 'Verificar'}</button>
              <button type="button" onClick={() => { setModo('inicio'); setMsg({ tipo: '', texto: '' }); }} className="btn-secondary">Volver</button>
            </div>
          </form>
        ) : (
          <div className="flex flex-col sm:flex-row gap-2 justify-center [&>button]:whitespace-nowrap [&>button]:text-sm">
            <button onClick={() => comprobar(true)} className="btn-primary flex items-center justify-center gap-2">
              <FaSyncAlt /> Ya lo confirmé
            </button>
            <button onClick={reenviar} disabled={ocupado} className="btn-secondary flex items-center justify-center gap-2">
              <FaPaperPlane /> {ocupado ? 'Enviando…' : 'Reenviar enlace'}
            </button>
            <button onClick={() => { setModo('codigo'); setMsg({ tipo: '', texto: '' }); }} className="btn-secondary flex items-center justify-center gap-2">
              <FaKey /> Tengo un código
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
