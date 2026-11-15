import { useState } from 'react';
import Topbar from './Topbar';
import Sidebar from './Sidebar';

const AppShell = ({
  brandName, brandIcon, brandLogo, panelTitle, home, menu = [], appLinks = [],
  user, onLogout, userApiBaseUrl = 'http://localhost:8082', children,
}) => {
  const [open, setOpen] = useState(() => typeof window !== 'undefined' && window.innerWidth >= 768);
  return (
    <div className="ui-shell min-h-screen bg-gray-50">
      <Topbar onToggleSidebar={() => setOpen(o => !o)} panelTitle={panelTitle} brandName={brandName}
        user={user} onLogout={onLogout} userApiBaseUrl={userApiBaseUrl} />
      <Sidebar open={open} onClose={() => { if (typeof window !== 'undefined' && window.innerWidth < 768) setOpen(false); }}
        brandName={brandName} brandIcon={brandIcon} brandLogo={brandLogo} home={home} menu={menu} appLinks={appLinks} />
      <main className={`pt-12 transition-all duration-300 ${open ? 'md:ml-60' : 'ml-0'}`}>
        <div className="container mx-auto px-4 py-5">{children}</div>
      </main>
    </div>
  );
};
export default AppShell;
