import { useState, useEffect } from 'react';
import { FaShareAlt, FaPlus, FaTrash, FaCopy, FaLock, FaLockOpen, FaEye, FaCalendar, FaToggleOff, FaToggleOn, FaWhatsapp, FaEnvelope, FaUsers } from 'react-icons/fa';
import shareLinkService from '../../services/shareLinkService';
import BulkSendToClients from './BulkSendToClients';

const ShareLinks = ({ listaId }) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(null);
  const [form, setForm] = useState({
    nombreEnlace: '',
    descripcion: '',
    password: '',
    fechaExpiracion: '',
    maxVistas: '',
    mostrarPrecios: true,
    mostrarStock: false,
    permitirBusqueda: true,
  });

  useEffect(() => { load(); }, [listaId]);

  const load = async () => {
    try { setLoading(true); setItems(await shareLinkService.listByList(listaId)); }
    catch (e) { console.error(e); } finally { setLoading(false); }
  };

  const submit = async (e) => {
    e.preventDefault(); setError('');
    try {
      await shareLinkService.create(listaId, {
        ...form,
        password: form.password || null,
        fechaExpiracion: form.fechaExpiracion ? form.fechaExpiracion + ':00' : null,
        maxVistas: form.maxVistas ? parseInt(form.maxVistas) : null,
      });
      setShowForm(false);
      setForm({ nombreEnlace: '', descripcion: '', password: '', fechaExpiracion: '', maxVistas: '',
                mostrarPrecios: true, mostrarStock: false, permitirBusqueda: true });
      load();
    } catch (err) { setError(err.response?.data?.message || 'Error al crear enlace'); }
  };

  const deactivate = async (enlaceId) => {
    try { await shareLinkService.deactivate(listaId, enlaceId); load(); }
    catch (e) { alert(e.response?.data?.message || 'Error'); }
  };

  const del = async (enlaceId) => {
    if (!window.confirm('¿Eliminar este enlace? No podra reactivarse.')) return;
    try { await shareLinkService.delete(listaId, enlaceId); load(); }
    catch (e) { alert(e.response?.data?.message || 'Error'); }
  };

  const copyToClipboard = (url, id) => {
    navigator.clipboard.writeText(url).then(() => {
      setCopied(id);
      setTimeout(() => setCopied(null), 2000);
    });
  };

  // ── Envío desde el backend (SendGrid / WhatsApp Cloud API) ────────────
  const [sendForm, setSendForm] = useState({});   // { [enlaceId]: {email, telefono, mensaje} }
  const [sending, setSending] = useState(null);    // `${id}-email` | `${id}-wa`
  const [sent, setSent] = useState(null);
  const [bulkFor, setBulkFor] = useState(null); // enlaceId del panel masivo abierto

  const upd = (id, campo, val) =>
    setSendForm(prev => ({ ...prev, [id]: { ...(prev[id] || {}), [campo]: val } }));

  const enviarEmail = async (e) => {
    const data = sendForm[e.id] || {};
    if (!data.email) { setError('Escribe un correo destinatario'); return; }
    setError(''); setSending(`${e.id}-email`);
    try {
      await shareLinkService.sendEmail(listaId, e.id, { destinatario: data.email, mensaje: data.mensaje || null });
      setSent(`${e.id}-email`); setTimeout(() => setSent(null), 2500);
    } catch (err) { setError(err.response?.data?.message || 'Error al enviar el correo'); }
    finally { setSending(null); }
  };

  const enviarWhatsApp = async (e) => {
    const data = sendForm[e.id] || {};
    if (!data.telefono) { setError('Escribe un número de WhatsApp (con indicativo país)'); return; }
    setError(''); setSending(`${e.id}-wa`);
    try {
      await shareLinkService.sendWhatsApp(listaId, e.id, { telefono: data.telefono, mensaje: data.mensaje || null });
      setSent(`${e.id}-wa`); setTimeout(() => setSent(null), 2500);
    } catch (err) { setError(err.response?.data?.message || 'Error al enviar el WhatsApp'); }
    finally { setSending(null); }
  };

  return (
    <div className="card mt-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold flex items-center gap-2"><FaShareAlt /> Enlaces compartibles</h2>
        <button onClick={() => setShowForm(!showForm)} className="btn-secondary flex items-center gap-2">
          <FaPlus /> {showForm ? 'Cancelar' : 'Nuevo enlace'}
        </button>
      </div>

      {showForm && (
        <div className="bg-gray-50 p-4 rounded mb-4">
          {error && <div className="bg-red-100 text-red-700 px-4 py-2 rounded mb-3">{error}</div>}
          <form onSubmit={submit} className="grid grid-cols-2 gap-3">
            <div className="col-span-2">
              <label className="block text-sm mb-1">Nombre del enlace</label>
              <input className="input-field" placeholder="Ej: Catalogo Mayoristas Q1 2026"
                value={form.nombreEnlace} onChange={e => setForm({...form, nombreEnlace: e.target.value})} />
            </div>
            <div className="col-span-2">
              <label className="block text-sm mb-1">Descripcion</label>
              <textarea className="input-field" rows="2" value={form.descripcion}
                onChange={e => setForm({...form, descripcion: e.target.value})} />
            </div>
            <div>
              <label className="block text-sm mb-1">Contraseña (opcional)</label>
              <input type="password" className="input-field" placeholder="Dejar vacio para sin clave"
                value={form.password} onChange={e => setForm({...form, password: e.target.value})} />
            </div>
            <div>
              <label className="block text-sm mb-1">Fecha de expiracion (opcional)</label>
              <input type="datetime-local" className="input-field"
                value={form.fechaExpiracion} onChange={e => setForm({...form, fechaExpiracion: e.target.value})} />
            </div>
            <div>
              <label className="block text-sm mb-1">Maximo de vistas (opcional)</label>
              <input type="number" min="1" className="input-field" placeholder="Sin limite"
                value={form.maxVistas} onChange={e => setForm({...form, maxVistas: e.target.value})} />
            </div>
            <div className="space-y-2 pt-6">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.mostrarPrecios}
                  onChange={e => setForm({...form, mostrarPrecios: e.target.checked})} />
                <span>Mostrar precios</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.mostrarStock}
                  onChange={e => setForm({...form, mostrarStock: e.target.checked})} />
                <span>Mostrar stock</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.permitirBusqueda}
                  onChange={e => setForm({...form, permitirBusqueda: e.target.checked})} />
                <span>Permitir busqueda</span>
              </label>
            </div>
            <div className="col-span-2">
              <button type="submit" className="btn-primary">Crear enlace</button>
            </div>
          </form>
        </div>
      )}

      {loading ? <p className="text-gray-500">Cargando...</p> : (
        <div className="space-y-3">
          {items.map(e => (
            <div key={e.id} className={`border rounded p-3 ${!e.activo ? 'opacity-50 bg-gray-50' : ''}`}>
              <div className="flex justify-between items-start mb-2">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <strong>{e.nombreEnlace || '(sin nombre)'}</strong>
                    {e.requierePassword ? <FaLock className="text-yellow-600" title="Protegido con contraseña" />
                                         : <FaLockOpen className="text-gray-400" title="Sin contraseña" />}
                    {!e.activo && <span className="px-2 py-0.5 bg-red-100 text-red-800 text-xs rounded">DESACTIVADO</span>}
                  </div>
                  {e.descripcion && <p className="text-sm text-gray-600 mb-2">{e.descripcion}</p>}
                  <div className="flex flex-wrap gap-3 text-xs text-gray-500">
                    <span className="flex items-center gap-1"><FaEye /> {e.vistasActuales || 0}{e.maxVistas ? `/${e.maxVistas}` : ''} vistas</span>
                    {e.fechaExpiracion && <span className="flex items-center gap-1"><FaCalendar /> Expira: {new Date(e.fechaExpiracion).toLocaleString()}</span>}
                    <span>Precios: {e.mostrarPrecios ? '✓' : '✗'}</span>
                    <span>Stock: {e.mostrarStock ? '✓' : '✗'}</span>
                  </div>
                </div>
                <div className="flex gap-2 ml-3">
                  {e.activo && (
                    <button onClick={() => deactivate(e.id)} className="text-xl text-green-600 hover:text-gray-400" title="Desactivar">
                      <FaToggleOn />
                    </button>
                  )}
                  <button onClick={() => del(e.id)} className="text-red-600 hover:text-red-800 text-xl" title="Eliminar"><FaTrash /></button>
                </div>
              </div>
              <div className="mt-2 bg-gray-100 p-2 rounded space-y-2">
                <div className="flex gap-2">
                  <input className="flex-1 bg-transparent text-sm font-mono" readOnly value={e.urlPublica} />
                  <button onClick={() => copyToClipboard(e.urlPublica, e.id)}
                    className="bg-primary-600 hover:bg-primary-700 text-white px-3 py-1 rounded text-sm flex items-center gap-1">
                    <FaCopy /> {copied === e.id ? '¡Copiado!' : 'Copiar'}
                  </button>
                </div>
                {e.activo && (
                  <div className="space-y-2 border-t pt-2">
                    <p className="text-xs text-gray-500 font-semibold">Enviar este catálogo:</p>
                    {/* Correo */}
                    <div className="flex gap-2">
                      <input type="email" placeholder="correo@cliente.com"
                        className="flex-1 input-field text-sm py-1"
                        value={(sendForm[e.id]?.email) || ''}
                        onChange={ev => upd(e.id, 'email', ev.target.value)} />
                      <button onClick={() => enviarEmail(e)} disabled={sending === `${e.id}-email`}
                        className="bg-gray-700 hover:bg-gray-800 text-white px-3 py-1 rounded text-sm flex items-center gap-1 whitespace-nowrap">
                        <FaEnvelope /> {sending === `${e.id}-email` ? 'Enviando...' : (sent === `${e.id}-email` ? '¡Enviado!' : 'Correo')}
                      </button>
                    </div>
                    {/* WhatsApp */}
                    <div className="flex gap-2">
                      <input type="tel" placeholder="573001234567 (con indicativo país)"
                        className="flex-1 input-field text-sm py-1"
                        value={(sendForm[e.id]?.telefono) || ''}
                        onChange={ev => upd(e.id, 'telefono', ev.target.value)} />
                      <button onClick={() => enviarWhatsApp(e)} disabled={sending === `${e.id}-wa`}
                        className="bg-[#25D366] hover:bg-[#1ebe5d] text-white px-3 py-1 rounded text-sm flex items-center gap-1 whitespace-nowrap">
                        <FaWhatsapp className="text-lg" /> {sending === `${e.id}-wa` ? 'Enviando...' : (sent === `${e.id}-wa` ? '¡Enviado!' : 'WhatsApp')}
                      </button>
                    </div>
                    {/* Mensaje opcional */}
                    <input type="text" placeholder="Mensaje opcional (se antepone)"
                      className="w-full input-field text-sm py-1"
                      value={(sendForm[e.id]?.mensaje) || ''}
                      onChange={ev => upd(e.id, 'mensaje', ev.target.value)} />
                    <button onClick={() => setBulkFor(e.id)}
                      className="w-full mt-1 bg-primary-600 hover:bg-primary-700 text-white px-3 py-2 rounded text-sm flex items-center justify-center gap-2 font-semibold">
                      <FaUsers /> Enviar a clientes (selección múltiple)
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
          {items.length === 0 && <p className="text-gray-500 text-center py-4">No hay enlaces compartibles para esta lista</p>}
        </div>
      )}
    
      {bulkFor && (
        <BulkSendToClients listaId={listaId} enlaceId={bulkFor} onClose={() => setBulkFor(null)} />
      )}
    </div>
  );
};

export default ShareLinks;
