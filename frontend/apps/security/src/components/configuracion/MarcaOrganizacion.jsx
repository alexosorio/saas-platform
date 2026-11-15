import { useState, useEffect } from 'react';
import { FaBuilding } from 'react-icons/fa';
import LogoUploader from '../common/LogoUploader';
import organizacionService from '../../services/organizacionService';

/** Logo de la organización: se usa en el menú, los correos y las listas de precios públicas. */
export default function MarcaOrganizacion() {
  const [marca, setMarca] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [msg, setMsg] = useState({ tipo: '', texto: '' });

  useEffect(() => { organizacionService.getMarca().then(setMarca).catch(() => setMarca({})); }, []);

  const cambiar = async (logo) => {
    setGuardando(true); setMsg({ tipo: '', texto: '' });
    try {
      const m = logo ? await organizacionService.setLogo(logo) : await organizacionService.quitarLogo();
      setMarca(m);
      setMsg({ tipo: 'ok', texto: logo ? 'Logo actualizado. Tus próximos correos ya lo incluirán.' : 'Logo eliminado.' });
    } catch (e) {
      setMsg({ tipo: 'error', texto: e.response?.status === 403
        ? 'No tienes permiso para cambiar el logo.' : (e.response?.data?.message || 'No se pudo guardar el logo') });
    } finally { setGuardando(false); }
  };

  if (!marca) return null;
  return (
    <div className="card mb-4">
      <h2 className="font-semibold text-primary-700 mb-1 flex items-center gap-2"><FaBuilding /> Organización</h2>
      <p className="text-sm text-gray-500 mb-4">
        {marca.nombre ? <><strong className="text-gray-700">{marca.nombre}</strong> · </> : null}
        El logo aparece en el menú, en los correos que se envían a tu equipo y clientes, y en las listas de precios que compartas.
      </p>
      <LogoUploader value={marca.logo} onChange={cambiar} disabled={guardando} />
      {msg.texto && <p className={`text-sm mt-3 ${msg.tipo === 'ok' ? 'text-green-700' : 'text-red-600'}`}>{msg.texto}</p>}
    </div>
  );
}
