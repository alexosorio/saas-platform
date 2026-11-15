import { useState } from 'react';
import { FaTimes, FaKey } from 'react-icons/fa';

// Cambia la contraseña contra el user-service (POST /api/users/change-password).
const ChangePasswordModal = ({ onClose, userApiBaseUrl }) => {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setError(''); setOk('');
    if (!form.currentPassword || !form.newPassword) { setError('Completa los campos'); return; }
    if (form.newPassword.length < 8) { setError('La nueva contraseña debe tener al menos 8 caracteres'); return; }
    if (form.newPassword !== form.confirm) { setError('Las contraseñas nuevas no coinciden'); return; }
    setLoading(true);
    try {
      const res = await fetch(`${userApiBaseUrl}/api/users/change-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}` },
        body: JSON.stringify({ currentPassword: form.currentPassword, newPassword: form.newPassword }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || 'No se pudo cambiar la contraseña');
      setOk(data.message || 'Contraseña actualizada correctamente');
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (e) { setError(e.message || 'Error'); } finally { setLoading(false); }
  };

  return (
    <div className="ui-shell fixed inset-0 bg-black/40 flex items-center justify-center z-[60] p-4">
      <div className="ui-mac-menu w-full max-w-md overflow-hidden">
        <div className="flex items-center px-4 py-2.5 border-b border-black/5 bg-white/60">
          <div className="flex gap-1.5">
            <span className="w-3 h-3 rounded-full" style={{ background: '#ff5f57' }} />
            <span className="w-3 h-3 rounded-full" style={{ background: '#febc2e' }} />
            <span className="w-3 h-3 rounded-full" style={{ background: '#28c840' }} />
          </div>
          <span className="flex-1 text-center text-sm font-medium text-gray-600 flex items-center justify-center gap-2">
            <FaKey className="text-gray-400" /> Cambiar contraseña
          </span>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700"><FaTimes /></button>
        </div>
        <div className="p-5 space-y-3">
          {error && <div className="bg-red-100 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          {ok && <div className="bg-green-100 text-green-700 px-3 py-2 rounded text-sm">{ok}</div>}
          <div>
            <label className="block text-sm font-medium mb-1">Contraseña actual</label>
            <input type="password" className="w-full px-4 py-2 border border-gray-300 rounded-lg outline-none focus:ring-2 focus:ring-primary-500"
              value={form.currentPassword} onChange={e => setForm({ ...form, currentPassword: e.target.value })} />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Nueva contraseña</label>
            <input type="password" className="w-full px-4 py-2 border border-gray-300 rounded-lg outline-none focus:ring-2 focus:ring-primary-500"
              value={form.newPassword} onChange={e => setForm({ ...form, newPassword: e.target.value })} />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Confirmar nueva contraseña</label>
            <input type="password" className="w-full px-4 py-2 border border-gray-300 rounded-lg outline-none focus:ring-2 focus:ring-primary-500"
              value={form.confirm} onChange={e => setForm({ ...form, confirm: e.target.value })} />
          </div>
          <div className="flex gap-2 pt-2">
            <button onClick={submit} disabled={loading}
              className="flex-1 bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded-lg disabled:opacity-50">
              {loading ? 'Guardando...' : 'Actualizar contraseña'}
            </button>
            <button onClick={onClose} className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium py-2 px-4 rounded-lg">Cerrar</button>
          </div>
        </div>
      </div>
    </div>
  );
};
export default ChangePasswordModal;
