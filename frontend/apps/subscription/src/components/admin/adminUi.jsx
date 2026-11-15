import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { FaTimes } from 'react-icons/fa';

export const money = (n, moneda = 'COP') =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: moneda || 'COP', maximumFractionDigits: 0 }).format(Number(n || 0));
export const fecha = (v) => { if (!v) return '—'; const [y, m, d] = String(v).slice(0, 10).split('-'); return `${d}/${m}/${y}`; };

export const ESTADO_SUSCRIPCION = {
  activa: ['Activa', 'bg-green-100 text-green-700'],
  periodo_prueba: ['En prueba', 'bg-primary-100 text-primary-700'],
  pendiente_pago: ['Pendiente de pago', 'bg-amber-100 text-amber-700'],
  suspendida: ['Suspendida', 'bg-red-100 text-red-700'],
  vencida: ['Vencida', 'bg-red-100 text-red-700'],
  cancelada: ['Cancelada', 'bg-gray-100 text-gray-600'],
};
export const ESTADO_PAGO = {
  pendiente: ['En validación', 'bg-amber-100 text-amber-700'],
  completado: ['Aprobado', 'bg-green-100 text-green-700'],
  fallido: ['Rechazado', 'bg-red-100 text-red-700'],
  reembolsado: ['Reembolsado', 'bg-gray-100 text-gray-600'],
  cancelado: ['Cancelado', 'bg-gray-100 text-gray-600'],
};
export const Badge = ({ mapa, valor }) => {
  const [txt, cls] = mapa[valor] || [valor, 'bg-gray-100 text-gray-600'];
  return <span className={`text-xs px-2 py-0.5 rounded whitespace-nowrap ${cls}`}>{txt}</span>;
};

/** Pestañas del módulo de administración */
export const AdminTabs = () => (
  <div className="flex gap-1 border-b mb-5">
    {[['/subscription/admin', 'Suscripciones', true], ['/subscription/admin/pagos', 'Validación de pagos'], ['/subscription/admin/facturacion', 'Facturación']]
      .map(([to, label, end]) => (
        <NavLink key={to} to={to} end={end}
          className={({ isActive }) => `px-4 py-2 text-sm -mb-px border-b-2 ${isActive ? 'border-primary-600 text-primary-700 font-medium' : 'border-transparent text-gray-500 hover:text-gray-700'}`}>
          {label}
        </NavLink>
      ))}
  </div>
);

/** Diálogo que pide un motivo antes de confirmar una acción */
export const MotivoModal = ({ titulo, descripcion, obligatorio = true, textoBoton, peligro, onConfirm, onClose }) => {
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const confirmar = async () => {
    if (obligatorio && !motivo.trim()) { setError('El motivo es obligatorio'); return; }
    setEnviando(true); setError('');
    try { await onConfirm(motivo.trim()); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo completar la acción'); setEnviando(false); }
  };
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex justify-between items-center px-5 py-3 border-b">
          <h3 className="font-bold">{titulo}</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700" aria-label="Cerrar"><FaTimes /></button>
        </div>
        <div className="p-5 space-y-3">
          {descripcion && <p className="text-sm text-gray-600">{descripcion}</p>}
          {error && <div className="bg-red-100 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          <div>
            <label className="block text-sm mb-1">Motivo{obligatorio ? ' *' : ' (opcional)'}</label>
            <textarea className="input-field" rows={3} value={motivo} onChange={e => setMotivo(e.target.value)} autoFocus />
          </div>
          <div className="flex gap-2 pt-1">
            <button onClick={confirmar} disabled={enviando}
              className={`flex-1 font-medium py-2 px-4 rounded-lg text-white disabled:opacity-50 ${peligro ? 'bg-red-600 hover:bg-red-700' : 'bg-primary-600 hover:bg-primary-700'}`}>
              {enviando ? 'Procesando…' : textoBoton}
            </button>
            <button onClick={onClose} className="btn-secondary">Cancelar</button>
          </div>
        </div>
      </div>
    </div>
  );
};
