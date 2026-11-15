import { useEffect, useRef, useState } from 'react';
import { tokenExpirado, renovarToken, forzarLogin } from '../services/api';
import parametroService from '../services/parametroService';

const CLAVE = 'ultimaActividad';
const AVISO_MS = 60 * 1000;   // aviso un minuto antes de cerrar

/**
 * Vigila la sesión:
 *  - cierre por INACTIVIDAD (minutos configurables en Configuración), con aviso previo;
 *    la actividad se comparte entre pestañas, así que trabajar en una mantiene viva la otra;
 *  - renueva el token cuando vence; si el servidor lo rechaza, cierra la sesión con aviso;
 *  - si se cierra sesión en otra pestaña, esta también sale.
 */
export default function SessionWatcher() {
  const [restante, setRestante] = useState(null);   // segundos para el cierre (solo durante el aviso)
  const limiteMs = useRef(30 * 60 * 1000);

  useEffect(() => {
    parametroService.sesion()
      .then(d => { if (d?.minutosInactividad > 0) limiteMs.current = d.minutosInactividad * 60 * 1000; })
      .catch(() => {});

    const marcar = () => localStorage.setItem(CLAVE, String(Date.now()));
    marcar();
    let ultimoMarcado = 0;
    const onActividad = () => {           // como máximo una escritura cada 5 s
      const ahora = Date.now();
      if (ahora - ultimoMarcado > 5000) { ultimoMarcado = ahora; marcar(); }
    };
    const eventos = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart'];
    eventos.forEach(e => window.addEventListener(e, onActividad, { passive: true }));

    let revisando = false;
    const check = async () => {
      if (window.location.pathname.startsWith('/login')) return;
      const at = localStorage.getItem('accessToken');
      if (!at) { forzarLogin('expired'); return; }

      // 1) Inactividad
      const inactivo = Date.now() - Number(localStorage.getItem(CLAVE) || Date.now());
      const falta = limiteMs.current - inactivo;
      if (falta <= 0) { forzarLogin('inactividad'); return; }
      setRestante(falta <= AVISO_MS ? Math.ceil(falta / 1000) : null);

      // 2) Token vencido: renovar
      if (!tokenExpirado(at) || revisando) return;
      revisando = true;
      try { await renovarToken(); }
      catch (e) { if (e.message !== 'red') forzarLogin('expired'); }
      finally { revisando = false; }
    };

    const onStorage = (e) => {
      if (e.key === 'accessToken' && !e.newValue && !window.location.pathname.startsWith('/login'))
        window.location.href = '/login?session=logout';
    };
    const onVisible = () => { if (document.visibilityState === 'visible') check(); };

    check();
    const id = setInterval(check, 5000);
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('storage', onStorage);
    return () => {
      clearInterval(id);
      eventos.forEach(e => window.removeEventListener(e, onActividad));
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('storage', onStorage);
    };
  }, []);

  if (restante == null) return null;
  return (
    <div className="fixed bottom-4 right-4 z-[70] bg-white border border-amber-300 shadow-lg rounded-lg p-4 max-w-sm" role="alert">
      <p className="text-sm text-gray-800">
        Tu sesión se cerrará en <strong>{restante} s</strong> por inactividad.
      </p>
      <button onClick={() => { localStorage.setItem(CLAVE, String(Date.now())); setRestante(null); }}
        className="btn-primary text-sm mt-2">Seguir conectado</button>
    </div>
  );
}
