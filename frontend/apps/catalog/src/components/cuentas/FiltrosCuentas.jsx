import { FaFilter, FaTimes } from 'react-icons/fa';
import { Autocomplete } from '@saas/ui-shell';

// Fecha local YYYY-MM-DD (toISOString usa UTC y en Colombia desplazaría el día después de las 7 p. m.)
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const hoy = () => new Date();

// Rangos rápidos de fecha
const PRESETS = [
  ['Hoy', () => [iso(hoy()), iso(hoy())]],
  ['Últimos 7 días', () => { const d = hoy(); d.setDate(d.getDate() - 6); return [iso(d), iso(hoy())]; }],
  ['Este mes', () => { const d = hoy(); return [iso(new Date(d.getFullYear(), d.getMonth(), 1)), iso(hoy())]; }],
  ['Últimos 90 días', () => { const d = hoy(); d.setDate(d.getDate() - 89); return [iso(d), iso(hoy())]; }],
];

const plural = (p) => (/[aeiou]$/.test(p) ? p + 's' : p + 'es');   // cliente → clientes, proveedor → proveedores

export const FILTROS_VACIOS = { estado: '', terceroId: '', terceroLabel: '', desde: '', hasta: '', campoFecha: 'creacion', soloVencidas: false };

/**
 * Filtros de cuentas por pagar / por cobrar.
 *   terceroSrc   : fuente del autocomplete (proveedores o clientes)
 *   terceroLabel : 'Proveedor' | 'Cliente'
 */
export default function FiltrosCuentas({ filtros, onChange, terceroSrc, terceroLabel }) {
  const set = (cambios) => onChange({ ...filtros, ...cambios });
  const activos = [filtros.estado, filtros.terceroId, filtros.desde, filtros.hasta, filtros.soloVencidas].filter(Boolean).length;

  return (
    <div className="card mb-4 space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-gray-500 flex items-center gap-1 mr-1"><FaFilter /> Estado:</span>
        {['', 'pendiente', 'parcial', 'pagada', 'anulada', 'saldo_a_favor'].map(e => (
          <button key={e} type="button" onClick={() => set({ estado: e })}
            className={`px-3 py-1 rounded text-sm ${filtros.estado === e ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700'}`}>
            {e === '' ? 'Todas' : e === 'saldo_a_favor' ? 'Saldo a favor' : e.charAt(0).toUpperCase() + e.slice(1)}
          </button>
        ))}
        <label className="ml-auto flex items-center gap-2 text-sm cursor-pointer">
          <input type="checkbox" checked={filtros.soloVencidas} onChange={e => set({ soloVencidas: e.target.checked })} />
          Solo vencidas
        </label>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
        <div className="md:col-span-4">
          <label className="block text-xs text-gray-500 mb-1">{terceroLabel}</label>
          <Autocomplete source={terceroSrc} value={filtros.terceroId} valueLabel={filtros.terceroLabel}
            onChange={(v, o) => set({ terceroId: v, terceroLabel: o?.label || '' })}
            placeholder={`Todos los ${plural(terceroLabel.toLowerCase())}`} />
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs text-gray-500 mb-1">Fecha de</label>
          <select className="input-field" value={filtros.campoFecha} onChange={e => set({ campoFecha: e.target.value })}>
            <option value="creacion">Registro</option>
            <option value="vencimiento">Vencimiento</option>
          </select>
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs text-gray-500 mb-1">Desde</label>
          <input type="date" className="input-field" value={filtros.desde} max={filtros.hasta || undefined}
            onChange={e => set({ desde: e.target.value })} />
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs text-gray-500 mb-1">Hasta</label>
          <input type="date" className="input-field" value={filtros.hasta} min={filtros.desde || undefined}
            onChange={e => set({ hasta: e.target.value })} />
        </div>
        <div className="md:col-span-2">
          <button type="button" onClick={() => onChange(FILTROS_VACIOS)} disabled={!activos}
            className="btn-secondary w-full flex items-center justify-center gap-2 disabled:opacity-40">
            <FaTimes /> Limpiar{activos ? ` (${activos})` : ''}
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {PRESETS.map(([label, rango]) => (
          <button key={label} type="button" onClick={() => { const [d, h] = rango(); set({ desde: d, hasta: h }); }}
            className="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">{label}</button>
        ))}
      </div>
    </div>
  );
}
