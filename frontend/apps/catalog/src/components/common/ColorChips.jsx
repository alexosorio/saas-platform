// Muestra los colores disponibles como chips con una muestra visual cuando el nombre es reconocible.
const MUESTRAS = [
  ['negro', '#111827'], ['blanco', '#ffffff'], ['gris', '#9ca3af'], ['grafito', '#4b5563'], ['plata', '#d1d5db'],
  ['plateado', '#d1d5db'], ['titanio', '#a8a29e'], ['dorado', '#d4a017'], ['oro', '#d4a017'], ['azul', '#2563eb'],
  ['celeste', '#7dd3fc'], ['rojo', '#dc2626'], ['verde', '#16a34a'], ['rosa', '#f472b6'], ['rosado', '#f472b6'],
  ['morado', '#7c3aed'], ['lila', '#c4b5fd'], ['violeta', '#7c3aed'], ['amarillo', '#facc15'], ['naranja', '#f97316'],
  ['beige', '#e7d8c1'], ['cafe', '#78350f'], ['café', '#78350f'], ['marron', '#78350f'], ['marrón', '#78350f'],
];
const muestra = (nombre) => {
  const n = nombre.toLowerCase();
  const m = MUESTRAS.find(([k]) => n.includes(k));
  return m ? m[1] : null;
};

export default function ColorChips({ colores = [], max = 6, vacio = null, size = 'sm' }) {
  if (!colores.length) return vacio;
  const visibles = colores.slice(0, max);
  const resto = colores.length - visibles.length;
  const pad = size === 'xs' ? 'px-1.5 py-0.5 text-[11px]' : 'px-2 py-0.5 text-xs';
  return (
    <div className="flex flex-wrap gap-1">
      {visibles.map(c => {
        const hex = muestra(c);
        return (
          <span key={c} className={`inline-flex items-center gap-1 rounded-full border border-gray-200 bg-white text-gray-700 ${pad}`}>
            {hex && <span className="w-2.5 h-2.5 rounded-full border border-black/10" style={{ background: hex }} />}
            {c}
          </span>
        );
      })}
      {resto > 0 && <span className={`rounded-full bg-gray-100 text-gray-500 ${pad}`}>+{resto}</span>}
    </div>
  );
}

const cop = (n) => '$' + Number(n).toLocaleString('es-CO', { maximumFractionDigits: 0 });

/** "$1.200.000", o "$1.100.000 – $1.300.000" si las unidades tienen precios distintos. */
export function rangoPrecio(min, max, sinPrecio = '—') {
  if (min == null) return sinPrecio;
  if (max != null && Number(max) > Number(min)) return `${cop(min)} – ${cop(max)}`;
  return cop(min);
}
