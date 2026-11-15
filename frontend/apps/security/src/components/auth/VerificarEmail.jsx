import { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { FaCheckCircle, FaEnvelopeOpenText, FaExclamationTriangle, FaPaperPlane, FaKey } from 'react-icons/fa';
import authService from '../../services/authService';
import { renovarToken } from '../../services/api';

/**
 * /verificar-email
 *  - Con ?token=…  : verifica con el enlace del correo. Si venció, permite pedir uno nuevo con un clic.
 *  - Sin token     : reenviar el enlace a un correo, o verificar escribiendo el código de 6 dígitos.
 */
// Fuera del componente: si se definiera adentro, se recrearía en cada tecla y los campos perderían el foco
const Contenedor = ({ children }) => (
  <div className="min-h-screen flex items-center justify-center bg-gray-100 py-8 px-4">
    <div className="card max-w-md w-full">{children}</div>
  </div>
);

const VerificarEmail = () => {
  const [params] = useSearchParams();
  const token = params.get('token');
  const haySesion = !!localStorage.getItem('accessToken');

  const [estado, setEstado] = useState(token ? 'verificando' : 'formulario'); // verificando | ok | vencido | invalido | formulario
  const [mensaje, setMensaje] = useState('');
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [modo, setModo] = useState('reenviar'); // reenviar | codigo
  const [email, setEmail] = useState(params.get('email') || '');
  const [codigo, setCodigo] = useState('');
  const yaIntentado = useRef(false);   // el enlace es de un solo uso: evita el doble llamado de StrictMode

  useEffect(() => {
    if (!token || yaIntentado.current) return;
    yaIntentado.current = true;
    authService.verificarEmail(token)
      .then((r) => { setMensaje(r.message); setEstado('ok'); if (haySesion) renovarToken().catch(() => {}); })
      .catch((err) => {
        const st = err.response?.status;
        setMensaje(err.response?.data?.message || 'No se pudo verificar el correo');
        setEstado(st === 410 ? 'vencido' : 'invalido');
      });
  }, [token]);

  const reenviar = async (e) => {
    e?.preventDefault();
    setError(''); setAviso(''); setEnviando(true);
    try {
      const r = await authService.reenviarVerificacion(estado === 'vencido' ? { token } : { email });
      setAviso(r.message);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo enviar el enlace. Intenta de nuevo.');
    } finally { setEnviando(false); }
  };

  const verificarCodigo = async (e) => {
    e.preventDefault();
    setError(''); setAviso(''); setEnviando(true);
    try {
      const r = await authService.verifyEmail(email.trim().toLowerCase(), codigo.trim());
      setMensaje(r.message || 'Correo verificado'); setEstado('ok');
      if (haySesion) renovarToken().catch(() => {});   // el token nuevo ya lleva el correo verificado
    } catch (err) {
      setError(err.response?.data?.message || 'Código inválido o vencido');
    } finally { setEnviando(false); }
  };

  const irA = haySesion
    ? <a href="/dashboard" className="btn-primary inline-block">Ir al inicio</a>
    : <Link to="/login" className="btn-primary inline-block">Iniciar sesión</Link>;

  if (estado === 'verificando') {
    return <Contenedor><p className="text-center text-gray-600 py-8">Verificando tu correo…</p></Contenedor>;
  }

  if (estado === 'ok') {
    return (
      <Contenedor>
        <div className="text-center py-4">
          <FaCheckCircle className="text-green-600 mx-auto mb-4" size={56} />
          <h2 className="text-2xl font-bold mb-2">Correo verificado</h2>
          <p className="text-gray-600 mb-6">{mensaje}</p>
          {irA}
        </div>
      </Contenedor>
    );
  }

  if (estado === 'vencido') {
    return (
      <Contenedor>
        <div className="text-center py-2">
          <FaExclamationTriangle className="text-amber-500 mx-auto mb-4" size={48} />
          <h2 className="text-2xl font-bold mb-2">El enlace expiró</h2>
          <p className="text-gray-600 mb-6">{mensaje}</p>
          {aviso
            ? <div className="bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded mb-4 text-sm">{aviso}</div>
            : <button onClick={reenviar} disabled={enviando} className="btn-primary inline-flex items-center gap-2">
                <FaPaperPlane /> {enviando ? 'Enviando…' : 'Enviarme un enlace nuevo'}
              </button>}
          {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mt-4 text-sm">{error}</div>}
          <p className="mt-6 text-sm"><Link to="/login" className="text-primary-600 hover:underline">Volver al inicio de sesión</Link></p>
        </div>
      </Contenedor>
    );
  }

  // 'invalido' o 'formulario'
  return (
    <Contenedor>
      <div className="text-center mb-6">
        <FaEnvelopeOpenText className="text-primary-600 mx-auto mb-3" size={44} />
        <h2 className="text-2xl font-bold">Verifica tu correo</h2>
        {estado === 'invalido'
          ? <p className="text-red-600 mt-2 text-sm">{mensaje}</p>
          : <p className="text-gray-600 mt-2 text-sm">¿No te llegó el correo o se venció el enlace? Pide uno nuevo o escribe el código que recibiste.</p>}
      </div>

      <div className="flex rounded-lg border border-gray-200 p-1 mb-5 text-sm">
        {[['reenviar', 'Reenviar enlace', FaPaperPlane], ['codigo', 'Tengo un código', FaKey]].map(([k, label, Icon]) => (
          <button key={k} type="button" onClick={() => { setModo(k); setError(''); setAviso(''); }}
            className={`flex-1 py-2 rounded-md flex items-center justify-center gap-2 ${modo === k ? 'bg-primary-600 text-white' : 'text-gray-600 hover:bg-gray-50'}`}>
            <Icon /> {label}
          </button>
        ))}
      </div>

      {aviso && <div className="bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded mb-4 text-sm">{aviso}</div>}
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4 text-sm">{error}</div>}

      {modo === 'reenviar' ? (
        <form onSubmit={reenviar} className="space-y-4">
          <div>
            <label className="block text-gray-700 mb-2">Correo registrado</label>
            <input type="email" className="input-field" value={email} required
              onChange={(e) => setEmail(e.target.value)} placeholder="tu@empresa.com" />
          </div>
          <button type="submit" disabled={enviando} className="btn-primary w-full flex items-center justify-center gap-2">
            <FaPaperPlane /> {enviando ? 'Enviando…' : 'Enviar enlace de verificación'}
          </button>
          <p className="text-xs text-gray-500">Revisa también la carpeta de correo no deseado. Puedes pedir un enlace nuevo cada minuto.</p>
        </form>
      ) : (
        <form onSubmit={verificarCodigo} className="space-y-4">
          <div>
            <label className="block text-gray-700 mb-2">Correo registrado</label>
            <input type="email" className="input-field" value={email} required
              onChange={(e) => setEmail(e.target.value)} placeholder="tu@empresa.com" />
          </div>
          <div>
            <label className="block text-gray-700 mb-2">Código de 6 dígitos</label>
            <input type="text" inputMode="numeric" maxLength={6} pattern="\d{6}" required
              className="input-field text-center font-mono text-2xl tracking-[0.5em]"
              value={codigo} onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))} placeholder="••••••" />
          </div>
          <button type="submit" disabled={enviando} className="btn-primary w-full">
            {enviando ? 'Verificando…' : 'Verificar'}
          </button>
        </form>
      )}

      <p className="text-center mt-6 text-sm">
        {haySesion
          ? <a href="/dashboard" className="text-primary-600 hover:underline">Volver al inicio</a>
          : <Link to="/login" className="text-primary-600 hover:underline">Volver al inicio de sesión</Link>}
      </p>
    </Contenedor>
  );
};

export default VerificarEmail;
