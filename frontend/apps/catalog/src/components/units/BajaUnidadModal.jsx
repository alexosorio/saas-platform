import { useState } from 'react';
import { FaTimes, FaExclamationTriangle } from 'react-icons/fa';
import unitService from '../../services/unitService';

const TIPOS = [
  { valor: 'error_registro', titulo: 'Error de registro',
    efecto: 'La unidad nunca debió ingresar. Se genera una nota crédito y la deuda con el proveedor se anula (o queda saldo a favor si ya se pagó).' },
  { valor: 'devolucion_proveedor', titulo: 'Devolución al proveedor',
    efecto: 'La unidad se devuelve al proveedor. Se genera una nota crédito y la deuda se anula (o queda saldo a favor si ya se pagó).' },
  { valor: 'dano_perdida', titulo: 'Daño o pérdida',
    efecto: 'La unidad sale del inventario, pero la deuda con el proveedor SE MANTIENE: se le sigue debiendo.' },
];

/** Baja de una unidad: nunca se elimina; queda el movimiento de inventario, la nota crédito y la auditoría. */
export default function BajaUnidadModal({ unidad, onClose, onDone }) {
  const [tipo, setTipo] = useState('');
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const seleccionado = TIPOS.find(t => t.valor === tipo);

  const confirmar = async () => {
    if (!tipo) { setError('Elige el tipo de baja'); return; }
    if (motivo.trim().length < 10) { setError('Describe el motivo (mínimo 10 caracteres)'); return; }
    setEnviando(true); setError('');
    try { await unitService.darDeBaja(unidad.id, tipo, motivo.trim()); onDone(); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo dar de baja la unidad'); setEnviando(false); }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg">
        <div className="flex justify-between items-center px-5 py-3 border-b">
          <h3 className="font-bold">Dar de baja la unidad <span className="font-mono">{unidad.codigoUnico}</span></h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700" aria-label="Cerrar"><FaTimes /></button>
        </div>
        <div className="p-5 space-y-4">
          {error && <div className="bg-red-100 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          <div>
            <p className="text-sm font-semibold mb-2">¿Por qué sale del inventario?</p>
            <div className="space-y-2">
              {TIPOS.map(t => (
                <label key={t.valor} className={`flex gap-3 border rounded-lg p-3 cursor-pointer ${tipo === t.valor ? 'border-primary-500 bg-primary-50' : 'border-gray-200'}`}>
                  <input type="radio" name="tipoBaja" className="mt-1" checked={tipo === t.valor} onChange={() => setTipo(t.valor)} />
                  <span><span className="font-medium text-sm">{t.titulo}</span>
                    <span className="block text-xs text-gray-600 mt-0.5">{t.efecto}</span></span>
                </label>
              ))}
            </div>
          </div>
          <div>
            <label className="block text-sm font-semibold mb-1">Observación *</label>
            <textarea className="input-field" rows={3} value={motivo} onChange={e => setMotivo(e.target.value)}
              placeholder="Ej: El IMEI se digitó en una unidad que no llegó en el pedido 4521." />
            <p className="text-xs text-gray-400 mt-1">Queda en el movimiento de inventario, en la nota crédito y en la auditoría.</p>
          </div>
          {seleccionado && (
            <div className="flex gap-2 text-xs bg-amber-50 text-amber-800 p-3 rounded">
              <FaExclamationTriangle className="mt-0.5 shrink-0" />
              <span>Esta acción no se puede deshacer. La unidad quedará en el historial como "dada de baja".</span>
            </div>
          )}
          <div className="flex gap-2">
            <button onClick={confirmar} disabled={enviando}
              className="flex-1 font-medium py-2 px-4 rounded-lg text-white bg-red-600 hover:bg-red-700 disabled:opacity-50">
              {enviando ? 'Procesando…' : 'Dar de baja'}
            </button>
            <button onClick={onClose} className="btn-secondary">Cancelar</button>
          </div>
        </div>
      </div>
    </div>
  );
}
