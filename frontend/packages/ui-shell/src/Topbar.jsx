import { useState, useEffect, useRef } from 'react';
import { FaBars, FaChevronDown, FaKey, FaSignOutAlt } from 'react-icons/fa';
import ChangePasswordModal from './ChangePasswordModal';

// Topbar a todo el ancho, SIEMPRE encima del sidebar (z-50).
const Topbar = ({ onToggleSidebar, panelTitle = 'Panel', brandName, user, onLogout, userApiBaseUrl }) => {
  const [openProfile, setOpenProfile] = useState(false);
  const [showPwd, setShowPwd] = useState(false);
  const ref = useRef(null);

  const u = user || JSON.parse(localStorage.getItem('user') || '{}');
  // Nombre y apellido del PERFIL; el correo solo si el perfil no tiene nombre
  const completo = [u?.nombre, u?.apellido].map(x => (x || '').trim()).filter(Boolean).join(' ');
  const nombre = completo || u?.name || (u?.email ? u.email.split('@')[0] : 'Usuario');
  const partes = (completo || nombre).split(/\s+/).filter(Boolean);
  // Iniciales: primer nombre + primer apellido (Alexander Osorio Barone → AO)
  const n0 = (u?.nombre || '').trim(), a0 = (u?.apellido || '').trim();
  const iniciales = (n0 && a0 ? n0[0] + a0[0]
    : partes.length > 1 ? partes[0][0] + partes[1][0] : (partes[0] || 'U').slice(0, 2)).toUpperCase();

  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpenProfile(false); };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);

  const logout = () => {
    try { onLogout?.(); } catch { /* el cierre continúa aunque falle el callback */ }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/login?session=logout_ok';   // recarga completa: limpia el estado de todos los módulos
  };

  return (
    <header className="ui-shell fixed top-0 left-0 right-0 h-12 bg-white ui-z1 z-50 flex items-center px-2 gap-1.5">
      {/* Perfil estilo macOS arriba a la izquierda */}
      <div className="relative" ref={ref}>
        <button onClick={() => setOpenProfile(o => !o)}
          className="flex items-center gap-1.5 pl-1 pr-2 py-1 rounded-full hover:bg-gray-100 transition-colors">
          <span className="w-7 h-7 rounded-full bg-primary-600 text-white flex items-center justify-center text-xs font-medium">{iniciales}</span>
          <span className="text-xs font-medium text-gray-700 max-w-[180px] truncate hidden sm:block">{nombre}</span>
          <FaChevronDown size={10} className="text-gray-400" />
        </button>
        {openProfile && (
          <div className="absolute left-0 top-full mt-2 w-64 ui-mac-menu overflow-hidden">
            <div className="flex items-center gap-1.5 px-3 py-1.5 border-b border-black/5">
              <span className="w-2.5 h-2.5 rounded-full" style={{ background: '#ff5f57' }} />
              <span className="w-2.5 h-2.5 rounded-full" style={{ background: '#febc2e' }} />
              <span className="w-2.5 h-2.5 rounded-full" style={{ background: '#28c840' }} />
            </div>
            <div className="px-3 py-2.5 flex items-center gap-2.5 border-b border-black/5">
              <span className="w-9 h-9 rounded-full bg-primary-600 text-white flex items-center justify-center text-sm font-medium">{iniciales}</span>
              <div className="min-w-0">
                <div className="text-sm font-medium text-gray-800 truncate">{nombre}</div>
                <div className="text-xs text-gray-500 truncate">{u?.email || 'Sin correo'}</div>
              </div>
            </div>
            <div className="py-1">
              <button onClick={() => { setShowPwd(true); setOpenProfile(false); }}
                className="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-primary-50 hover:text-primary-700 flex items-center gap-2.5">
                <FaKey className="text-gray-400" /> Cambiar contraseña
              </button>
              <button onClick={logout}
                className="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center gap-2.5">
                <FaSignOutAlt /> Cerrar sesión
              </button>
            </div>
          </div>
        )}
      </div>

      <button onClick={onToggleSidebar}
        className="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-600"
        title="Mostrar/ocultar menú" aria-label="Menú"><FaBars size={15} /></button>

      <span className="text-gray-200">|</span>
      <span className="text-sm text-gray-700 font-medium truncate">{panelTitle || brandName}</span>
    </header>
  );
};
export default Topbar;
