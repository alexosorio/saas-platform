import { NavLink } from 'react-router-dom';
import { FaBoxOpen, FaHome } from 'react-icons/fa';

// Sidebar debajo del topbar (top-12). No tapa la hamburguesa.
const linkClass = ({ isActive }) =>
  'ui-side-link ' + (isActive
    ? 'bg-primary-100 text-primary-700 font-medium'
    : 'text-gray-700 hover:bg-primary-50 hover:text-primary-700');
const activeStyle = ({ isActive }) => (isActive ? { boxShadow: 'inset 3px 0 0 0 currentColor' } : undefined);

const Sidebar = ({ open, onClose, brandName = 'Panel', brandIcon, brandLogo, home, menu = [], appLinks = [] }) => (
  <>
    {open && <div className="fixed inset-0 top-12 bg-black/40 z-30 md:hidden" onClick={onClose} />}
    <aside className={`ui-shell fixed top-12 left-0 bottom-0 z-40 w-60 bg-white ui-z1 flex flex-col
      transform transition-transform duration-300 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
      {brandLogo ? (
        // Logo de la organización (data URL o URL)
        <div className="h-16 flex items-center gap-2 px-4 border-b border-gray-100" title={brandName}>
          <img src={brandLogo} alt={brandName} className="max-h-11 max-w-[200px] w-auto object-contain" />
        </div>
      ) : (
        <div className="h-9 flex items-center gap-2 px-4 text-primary-700 border-b border-gray-100">
          <span className="text-base">{brandIcon || <FaBoxOpen />}</span>
          <span className="text-sm font-semibold truncate">{brandName}</span>
        </div>
      )}

      <nav className="flex-1 overflow-y-auto py-1.5">
        {home && (
          <NavLink to={home.to} end onClick={onClose} className={linkClass} style={activeStyle}>
            {home.icon || <FaHome />} {home.label || 'Inicio'}
          </NavLink>
        )}
        {menu.map((sec) => (
          <div key={sec.label}>
            <div className="ui-section-label text-gray-400">{sec.label}</div>
            {sec.items.map((it) => (
              <NavLink key={it.to} to={it.to} onClick={onClose} className={linkClass} style={activeStyle}>
                {it.icon} {it.label}
              </NavLink>
            ))}
          </div>
        ))}
        {appLinks.length > 0 && (
          <>
            <div className="ui-section-label text-gray-400">Otras apps</div>
            {appLinks.map((a) => (
              <a key={a.href} href={a.href} className="ui-side-link text-gray-700 hover:bg-primary-50 hover:text-primary-700">
                {a.icon} {a.label}
              </a>
            ))}
          </>
        )}
      </nav>
      <div className="px-4 py-2 text-[11px] text-gray-400 border-t">SaaS Platform</div>
    </aside>
  </>
);
export default Sidebar;
