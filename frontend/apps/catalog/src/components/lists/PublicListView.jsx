import { useState, useEffect } from 'react';
import ColorChips from '../common/ColorChips';
import { useParams } from 'react-router-dom';
import { FaSearch, FaLock, FaBoxOpen, FaWhatsapp, FaEnvelope, FaShareAlt } from 'react-icons/fa';
import shareLinkService from '../../services/shareLinkService';

const USER_API = import.meta.env.VITE_USER_API_URL || 'http://localhost:8082';

const PublicListView = () => {
  const { token } = useParams();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [needsPassword, setNeedsPassword] = useState(false);
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [search, setSearch] = useState('');

  useEffect(() => { tryView(); }, [token]);

  const tryView = async () => {
    try {
      setLoading(true);
      const v = await shareLinkService.viewPublic(token);
      setData(v); setError('');
    } catch (err) {
      const status = err.response?.status;
      const msg = err.response?.data?.message || 'Error';
      if (msg.toLowerCase().includes('contraseña') || status === 400) {
        setNeedsPassword(true);
      } else if (status === 404) {
        setError('Este enlace no existe.');
      } else {
        setError(msg);
      }
    } finally { setLoading(false); }
  };

  const submitPassword = async (e) => {
    e.preventDefault(); setError(''); setSubmitting(true);
    try {
      const v = await shareLinkService.viewPublicWithPassword(token, password);
      setData(v); setNeedsPassword(false);
    } catch (err) {
      setError(err.response?.data?.message || 'Contraseña incorrecta');
    } finally { setSubmitting(false); }
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center text-gray-500">Cargando...</div>;
  }

  // Pantalla de contraseña
  if (needsPassword) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
        <div className="card max-w-md w-full text-center">
          <FaLock className="mx-auto text-yellow-500 mb-4" size={48} />
          <h2 className="text-2xl font-bold mb-2">Enlace protegido</h2>
          <p className="text-gray-600 mb-6">Este catalogo requiere contraseña para ser visto</p>
          {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
          <form onSubmit={submitPassword} className="space-y-4">
            <input type="password" autoFocus className="input-field"
              placeholder="Contraseña" value={password}
              onChange={e => setPassword(e.target.value)} required />
            <button type="submit" disabled={submitting} className="btn-primary w-full">
              {submitting ? 'Verificando...' : 'Entrar'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Error fatal (enlace inexistente, expirado, desactivado)
  if (error && !data) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
        <div className="card max-w-md w-full text-center">
          <h2 className="text-2xl font-bold mb-2">No disponible</h2>
          <p className="text-gray-600">{error}</p>
        </div>
      </div>
    );
  }

  if (!data) return null;

  const filtered = search && data.permitirBusqueda
    ? data.items.filter(it =>
        (it.nombre || '').toLowerCase().includes(search.toLowerCase()) ||
        (it.sku || '').toLowerCase().includes(search.toLowerCase()) ||
        (it.marcaNombre || '').toLowerCase().includes(search.toLowerCase())
      )
    : data.items;

  // Compartir la lista (la URL actual) o un producto puntual
  const paginaUrl = window.location.href;
  const compartirListaWhatsApp = () => {
    const msg = `Hola 👋, te comparto ${data.nombreLista}:\n\n${paginaUrl}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank', 'noopener');
  };
  const compartirListaEmail = () => {
    const asunto = `Lista de precios: ${data.nombreLista}`;
    const cuerpo = `Hola, te comparto nuestra lista de precios:\n\n${paginaUrl}`;
    window.location.href = `mailto:?subject=${encodeURIComponent(asunto)}&body=${encodeURIComponent(cuerpo)}`;
  };
  const compartirProductoWhatsApp = (it) => {
    const variable = it.precioHasta != null && Number(it.precioHasta) > Number(it.precio);
    const precio = (data.mostrarPrecios && it.precio != null) ? ` — ${variable ? 'desde ' : ''}$${Number(it.precio).toLocaleString()}` : '';
    const colores = it.colores?.length ? `Colores: ${it.colores.join(', ')}\n` : '';
    const msg = `${it.nombre}${precio}\n${colores}${it.sku ? 'SKU: ' + it.sku + '\n' : ''}\nVer catálogo: ${paginaUrl}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank', 'noopener');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-primary-600 text-white py-8 px-4 shadow">
        <div className="container mx-auto max-w-6xl">
          {(data.tieneLogo || data.organizacionNombre) && (
            <div className="flex items-center gap-3 mb-4">
              {data.tieneLogo && (
                <span className="bg-white rounded-lg px-3 py-2 shadow-sm inline-flex">
                  <img src={`${USER_API}/api/public/organizaciones/${data.organizacionId}/logo`}
                    alt={data.organizacionNombre || 'Logo'} className="h-10 md:h-12 w-auto max-w-[200px] object-contain"
                    onError={(e) => { e.currentTarget.parentElement.style.display = 'none'; }} />
                </span>
              )}
              {data.organizacionNombre && <span className="text-primary-100 font-semibold text-lg">{data.organizacionNombre}</span>}
            </div>
          )}
          <h1 className="text-3xl md:text-4xl font-bold flex items-center gap-3">
            <FaBoxOpen /> {data.nombreLista}
          </h1>
          {data.nombreEnlace && <p className="text-primary-100 mt-1 text-lg">{data.nombreEnlace}</p>}
          {data.descripcion && <p className="text-primary-100 mt-2">{data.descripcion}</p>}
          {data.tipoCliente && (
            <span className="inline-block mt-3 px-3 py-1 bg-white text-primary-700 rounded-full text-xs font-semibold uppercase">
              {data.tipoCliente}
            </span>
          )}
          <div className="flex gap-2 mt-4">
            <button onClick={compartirListaWhatsApp}
              className="bg-[#25D366] hover:bg-[#1ebe5d] text-white px-4 py-2 rounded text-sm flex items-center gap-2 font-semibold">
              <FaWhatsapp className="text-lg" /> Compartir por WhatsApp
            </button>
            <button onClick={compartirListaEmail}
              className="bg-white/20 hover:bg-white/30 text-white px-4 py-2 rounded text-sm flex items-center gap-2 font-semibold">
              <FaEnvelope /> Compartir por correo
            </button>
          </div>
        </div>
      </div>

      <div className="container mx-auto max-w-6xl py-6 px-4">
        {/* Buscador */}
        {data.permitirBusqueda && (
          <div className="card mb-6">
            <div className="flex items-center gap-3">
              <FaSearch className="text-gray-400" />
              <input type="text" placeholder="Buscar por nombre, SKU o marca..."
                value={search} onChange={e => setSearch(e.target.value)}
                className="input-field flex-1" />
            </div>
          </div>
        )}

        <p className="text-gray-500 text-sm mb-4">
          Mostrando {filtered.length} de {data.items.length} productos
        </p>

        {/* Grid de productos */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(it => (
            <div key={it.productoId} className="card hover:shadow-lg transition">
              {it.imagenUrl && (
                <img src={it.imagenUrl.startsWith('http') ? it.imagenUrl : (import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8083') + it.imagenUrl}
                     alt={it.nombre} className="w-full h-48 object-cover rounded mb-3" />
              )}
              <h3 className="font-bold text-lg">{it.nombre}</h3>
              <div className="flex flex-wrap gap-2 my-2 text-xs">
                {it.marcaNombre && <span className="bg-gray-100 text-gray-700 px-2 py-1 rounded">{it.marcaNombre}</span>}
                {it.categoriaNombre && <span className="bg-primary-100 text-primary-700 px-2 py-1 rounded">{it.categoriaNombre}</span>}
              </div>
              {it.colores?.length > 0 && (
                <div className="mt-2">
                  <p className="text-xs text-gray-500 mb-1">Colores disponibles</p>
                  <ColorChips colores={it.colores} max={8} />
                </div>
              )}
              {it.sku && <p className="text-xs text-gray-500 font-mono mt-2">SKU: {it.sku}</p>}
              {it.descripcion && <p className="text-sm text-gray-600 mt-2 line-clamp-2">{it.descripcion}</p>}
              <div className="mt-3 pt-3 border-t flex justify-between items-end">
                {data.mostrarPrecios && it.precio != null ? (
                  <div className="text-primary-600">
                    {it.precioHasta != null && Number(it.precioHasta) > Number(it.precio) &&
                      <span className="block text-xs text-gray-500 font-normal">Desde</span>}
                    <span className="text-2xl font-bold">${Number(it.precio).toLocaleString()}</span>
                  </div>
                ) : <span className="text-gray-400 italic">Precio a consultar</span>}
                {data.mostrarStock && it.stockTotal != null && (
                  <span className={`text-sm font-semibold ${it.stockTotal > 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {it.stockTotal > 0 ? `Stock: ${it.stockTotal}` : 'Sin stock'}
                  </span>
                )}
              </div>
              <button onClick={() => compartirProductoWhatsApp(it)}
                className="mt-3 w-full bg-[#25D366] hover:bg-[#1ebe5d] text-white px-3 py-2 rounded text-sm flex items-center justify-center gap-2 font-semibold">
                <FaWhatsapp className="text-lg" /> Compartir este producto
              </button>
            </div>
          ))}
        </div>

        {filtered.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            {search ? 'No se encontraron productos con esa busqueda' : 'Esta lista no tiene productos publicados'}
          </div>
        )}

        <div className="mt-12 text-center text-xs text-gray-400 pb-6">
          Lista compartida desde la plataforma SaaS · Solo lectura
        </div>
      </div>
    </div>
  );
};

export default PublicListView;
