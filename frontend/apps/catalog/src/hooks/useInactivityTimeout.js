import { useEffect, useRef } from 'react';

/**
 * Cierra la sesion (limpia localStorage y manda a /login) tras N
 * minutos sin actividad de raton/teclado.
 *
 * @param {number} minutos - Default 30 minutos.
 * @param {Function} onTimeout - Opcional; si no se pasa, hace logout estandar.
 */
export default function useInactivityTimeout(minutos = 30, onTimeout) {
  const timer = useRef(null);

  useEffect(() => {
    const ms = minutos * 60 * 1000;

    const doLogout = () => {
      if (onTimeout) return onTimeout();
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      // /login esta servido por la app de seguridad bajo el mismo origen (proxy)
      window.location.href = '/login?inactividad=1';
    };

    const reset = () => {
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(doLogout, ms);
    };

    // Eventos que cuentan como actividad
    const events = ['mousemove', 'mousedown', 'keypress', 'touchstart', 'scroll', 'click'];
    events.forEach(e => window.addEventListener(e, reset, { passive: true }));

    reset();   // arranca el contador

    return () => {
      if (timer.current) clearTimeout(timer.current);
      events.forEach(e => window.removeEventListener(e, reset));
    };
  }, [minutos]);
}
