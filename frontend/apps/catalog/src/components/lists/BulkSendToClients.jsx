import { useState, useEffect, useMemo } from 'react';
import { FaWhatsapp, FaEnvelope, FaSearch, FaCheck, FaTimes, FaUsers, FaPaperPlane } from 'react-icons/fa';
import clientService from '../../services/clientService';
import shareLinkService from '../../services/shareLinkService';

// Envío masivo del enlace a clientes seleccionados (correo y/o WhatsApp).
const BulkSendToClients = ({ listaId, enlaceId, onClose }) => {
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(new Set());
  const [canal, setCanal] = useState('ambos');   // email | whatsapp | ambos
  const [mensaje, setMensaje] = useState('');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    clientService.getAll()
      .then(data => setClients(data.filter(c => c.activo)))
      .catch(() => setError('No se pudieron cargar los clientes'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return clients;
    return clients.filter(c =>
      (c.nombre || '').toLowerCase().includes(q) ||
      (c.email || '').toLowerCase().includes(q) ||
      (c.telefono || '').includes(q));
  }, [clients, search]);

  // Sólo se pueden seleccionar clientes que tengan el dato del canal elegido
  const tieneCanal = (c) => {
    if (canal === 'email') return !!c.email;
    if (canal === 'whatsapp') return !!c.telefono;
    return !!c.email || !!c.telefono; // ambos: con al menos uno
  };

  const toggle = (id) => {
    const n = new Set(selected);
    n.has(id) ? n.delete(id) : n.add(id);
    setSelected(n);
  };

  const seleccionables = filtered.filter(tieneCanal);
  const allSelected = seleccionables.length > 0 && seleccionables.every(c => selected.has(c.id));
  const toggleAll = () => {
    if (allSelected) setSelected(new Set());
    else setSelected(new Set(seleccionables.map(c => c.id)));
  };

  const enviar = async () => {
    const ids = [...selected];
    if (ids.length === 0) { setError('Selecciona al menos un cliente'); return; }
    setError(''); setSending(true); setResult(null);
    try {
      const r = await shareLinkService.sendBulk(listaId, enlaceId, { clienteIds: ids, canal, mensaje: mensaje || null });
      setResult(r);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al enviar');
    } finally { setSending(false); }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center px-5 py-3 border-b">
          <h3 className="text-lg font-bold flex items-center gap-2"><FaUsers className="text-primary-600" /> Enviar a clientes</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700 text-xl"><FaTimes /></button>
        </div>

        {/* Canal */}
        <div className="px-5 py-3 border-b">
          <p className="text-sm font-semibold mb-2">Canal de envío</p>
          <div className="flex gap-2">
            {[
              { v: 'ambos', label: 'Correo + WhatsApp', icon: null },
              { v: 'email', label: 'Solo correo', icon: <FaEnvelope /> },
              { v: 'whatsapp', label: 'Solo WhatsApp', icon: <FaWhatsapp /> },
            ].map(opt => (
              <button key={opt.v} onClick={() => { setCanal(opt.v); setSelected(new Set()); }}
                className={`flex-1 px-3 py-2 rounded text-sm font-semibold flex items-center justify-center gap-1 border
                  ${canal === opt.v ? 'bg-primary-600 text-white border-primary-600' : 'bg-white text-gray-700 border-gray-300'}`}>
                {opt.icon} {opt.label}
              </button>
            ))}
          </div>
        </div>

        {result ? (
          /* Resultados */
          <div className="px-5 py-4 overflow-y-auto">
            <div className="grid grid-cols-3 gap-3 mb-4">
              <div className="bg-green-50 rounded p-3 text-center"><div className="text-2xl font-bold text-green-600">{result.enviados}</div><div className="text-xs text-gray-600">Enviados</div></div>
              <div className="bg-yellow-50 rounded p-3 text-center"><div className="text-2xl font-bold text-yellow-600">{result.omitidos}</div><div className="text-xs text-gray-600">Omitidos</div></div>
              <div className="bg-red-50 rounded p-3 text-center"><div className="text-2xl font-bold text-red-600">{result.fallidos}</div><div className="text-xs text-gray-600">Fallidos</div></div>
            </div>
            <div className="border rounded divide-y max-h-64 overflow-y-auto text-sm">
              {result.detalle.map((d, i) => (
                <div key={i} className="flex items-center justify-between px-3 py-2">
                  <div className="flex items-center gap-2">
                    {d.canal === 'whatsapp' ? <FaWhatsapp className="text-[#25D366]" /> : <FaEnvelope className="text-gray-500" />}
                    <span>{d.nombre}</span>
                    {d.destino && <span className="text-gray-400 text-xs">{d.destino}</span>}
                  </div>
                  <span className={`text-xs font-semibold px-2 py-0.5 rounded ${
                    d.estado === 'ENVIADO' ? 'bg-green-100 text-green-700' :
                    d.estado === 'OMITIDO' ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'}`}
                    title={d.motivo || ''}>
                    {d.estado}{d.motivo ? ` · ${d.motivo}` : ''}
                  </span>
                </div>
              ))}
            </div>
            <div className="flex gap-2 mt-4">
              <button onClick={() => { setResult(null); }} className="btn-secondary flex-1">Enviar a otros</button>
              <button onClick={onClose} className="btn-primary flex-1">Cerrar</button>
            </div>
          </div>
        ) : (
          <>
            {/* Búsqueda + seleccionar todos */}
            <div className="px-5 py-3 border-b">
              <div className="relative mb-2">
                <FaSearch className="absolute left-3 top-3 text-gray-400" />
                <input className="input-field pl-9" placeholder="Buscar cliente por nombre, correo o teléfono..."
                  value={search} onChange={e => setSearch(e.target.value)} />
              </div>
              <div className="flex justify-between items-center text-sm">
                <button onClick={toggleAll} className="text-primary-600 font-semibold">
                  {allSelected ? 'Quitar todos' : 'Seleccionar todos los visibles'}
                </button>
                <span className="text-gray-500">{selected.size} seleccionado(s)</span>
              </div>
            </div>

            {/* Lista de clientes */}
            <div className="flex-1 overflow-y-auto px-5 py-2">
              {loading ? <p className="text-center text-gray-500 py-6">Cargando clientes...</p> :
               filtered.length === 0 ? <p className="text-center text-gray-500 py-6">No hay clientes.</p> :
               filtered.map(c => {
                 const habilitado = tieneCanal(c);
                 return (
                   <label key={c.id}
                     className={`flex items-center gap-3 py-2 px-2 rounded ${habilitado ? 'cursor-pointer hover:bg-gray-50' : 'opacity-50 cursor-not-allowed'}`}>
                     <input type="checkbox" disabled={!habilitado}
                       checked={selected.has(c.id)} onChange={() => habilitado && toggle(c.id)} />
                     <div className="flex-1">
                       <div className="font-medium">{c.nombre} <span className="text-xs text-gray-400">({c.tipo})</span></div>
                       <div className="text-xs text-gray-500 flex gap-3">
                         <span className={c.email ? '' : 'text-gray-300'}><FaEnvelope className="inline mr-1" />{c.email || 'sin correo'}</span>
                         <span className={c.telefono ? '' : 'text-gray-300'}><FaWhatsapp className="inline mr-1" />{c.telefono || 'sin teléfono'}</span>
                       </div>
                     </div>
                     {selected.has(c.id) && <FaCheck className="text-primary-600" />}
                   </label>
                 );
               })}
            </div>

            {/* Mensaje + enviar */}
            <div className="px-5 py-3 border-t space-y-2">
              {error && <div className="bg-red-100 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
              <input className="input-field text-sm" placeholder="Mensaje opcional (se antepone)"
                value={mensaje} onChange={e => setMensaje(e.target.value)} />
              <button onClick={enviar} disabled={sending || selected.size === 0}
                className="btn-primary w-full flex items-center justify-center gap-2">
                <FaPaperPlane /> {sending ? 'Enviando...' : `Enviar a ${selected.size} cliente(s)`}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default BulkSendToClients;
