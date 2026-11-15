import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8082';
const USER_API = import.meta.env.VITE_USER_API_URL || import.meta.env.VITE_API_URL || 'http://localhost:8082';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// ── Sesión: decodifica JWT (base64url) y maneja expiración/refresh ────
export function tokenExpirado(token) {
  try {
    let b = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b.length % 4) b += '=';
    const p = JSON.parse(atob(b));
    return p.exp && Math.floor(Date.now() / 1000) >= p.exp;
  } catch { return false; }
}

export function forzarLogin(motivo = 'expired') {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = `/login?session=${motivo}`;
  }
}

// Renueva el access token. Distingue:
//   'rechazado' -> el servidor no acepta el refresh token (sesión terminada)
//   'red'       -> no hubo respuesta (backend caído); NO debe cerrar la sesión
let refreshPromise = null;
export function renovarToken() {
  const rt = localStorage.getItem('refreshToken');
  if (!rt || tokenExpirado(rt)) return Promise.reject(new Error('rechazado'));
  if (!refreshPromise) {
    refreshPromise = fetch(`${USER_API}/api/auth/refresh`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${rt}` },
    })
      .catch(() => { throw new Error('red'); })
      .then(async (r) => {
        if (r.status === 401 || r.status === 403) throw new Error('rechazado');
        if (!r.ok) throw new Error('red');
        const d = await r.json();
        if (!d.accessToken) throw new Error('rechazado');
        localStorage.setItem('accessToken', d.accessToken);
        if (d.refreshToken) localStorage.setItem('refreshToken', d.refreshToken);
        return d.accessToken;
      })
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}

async function obtenerTokenValido() {
  const at = localStorage.getItem('accessToken');
  if (!at || !tokenExpirado(at)) return at;
  try { return await renovarToken(); }
  catch (e) {
    if (e.message === 'rechazado') { forzarLogin(); throw new axios.Cancel('sesion_expirada'); }
    return at; // error de red: se intenta igual; si el backend responde 401 se maneja abajo
  }
}

api.interceptors.request.use(
  async (config) => {
    const token = await obtenerTokenValido();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

// 401 = sesión no válida -> renovar una vez; si no se puede, cerrar sesión con aviso.
// 403 = sesión válida sin permiso -> NO cierra sesión.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config || {};
    // Correo sin confirmar: la app muestra la pantalla de verificación (VerificacionGate)
    if (error.response?.status === 403 && error.response?.data?.codigo === 'EMAIL_NO_VERIFICADO') {
      window.dispatchEvent(new Event('correo-sin-verificar'));
    }
    if (error.response?.status === 401 && localStorage.getItem('accessToken')) {
      if (!original._retry) {
        original._retry = true;
        try {
          const nuevo = await renovarToken();
          original.headers = original.headers || {};
          original.headers.Authorization = `Bearer ${nuevo}`;
          return api(original);
        } catch (e) {
          if (e.message !== 'red') forzarLogin();
        }
      } else {
        forzarLogin();
      }
    }
    return Promise.reject(error);
  }
);

export default api;
