import { useState, useEffect, useRef, useCallback } from 'react';
import { FaTimes, FaChevronDown } from 'react-icons/fa';

// Normaliza para buscar sin importar mayúsculas ni tildes.
const norm = (s) => String(s ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

/**
 * Fuente "diferida": descarga la lista completa SOLO cuando el usuario interactúa
 * (no al abrir la página), la cachea y filtra localmente. Para catálogos pequeños.
 */
export function lazyOptions(fetchAll, { label = 'nombre', value = 'id', sublabel, filter } = {}) {
  let cache = null;
  const toOpt = (it) => ({
    value: it[value],
    label: typeof label === 'function' ? label(it) : it[label],
    sublabel: sublabel ? (typeof sublabel === 'function' ? sublabel(it) : it[sublabel]) : undefined,
    raw: it,   // registro completo, para lógica dependiente (p. ej. tipo de cliente)
  });
  const all = () => {
    if (!cache) {
      cache = Promise.resolve(fetchAll())
        .then((list) => (filter ? list.filter(filter) : list).map(toOpt))
        .catch((e) => { cache = null; throw e; });
    }
    return cache;
  };
  return {
    minChars: 0,
    loadOptions: async (q) => {
      const opts = await all();
      const nq = norm(q);
      return (nq ? opts.filter((o) => norm(o.label).includes(nq) || norm(o.sublabel).includes(nq)) : opts).slice(0, 50);
    },
    resolveLabel: async (v) => (await all()).find((o) => String(o.value) === String(v))?.label,
  };
}

/**
 * Fuente "remota": consulta el servidor con lo que el usuario escribe.
 * Para listas grandes (clientes, proveedores, ciudades).
 */
export function remoteOptions(search, { label = 'nombre', value = 'id', sublabel, getById, minChars = 2 } = {}) {
  const toOpt = (it) => ({
    value: it[value],
    label: typeof label === 'function' ? label(it) : it[label],
    sublabel: sublabel ? (typeof sublabel === 'function' ? sublabel(it) : it[sublabel]) : undefined,
    raw: it,   // registro completo, para lógica dependiente (p. ej. tipo de cliente)
  });
  return {
    minChars,
    loadOptions: async (q) => (await search(q)).map(toOpt),
    resolveLabel: getById ? async (v) => { const it = await getById(v); return it ? toOpt(it).label : undefined; } : undefined,
  };
}

/**
 * Autocomplete bajo demanda. Reemplaza <select> cuyas opciones vienen de la base de datos.
 *   source      : resultado de lazyOptions(...) o remoteOptions(...)
 *   value       : id seleccionado ('' o null = nada)
 *   onChange    : (value, option) => void
 *   valueLabel  : etiqueta conocida del valor actual (evita una consulta en modo edición)
 */
export default function Autocomplete({
  source, value, onChange, valueLabel, placeholder = 'Buscar…',
  required = false, disabled = false, clearable = true, className = '', debounce = 300,
}) {
  const [text, setText] = useState(valueLabel || '');
  const [label, setLabel] = useState(valueLabel || '');
  const [open, setOpen] = useState(false);
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [active, setActive] = useState(-1);
  const boxRef = useRef(null);
  const reqId = useRef(0);
  const minChars = source?.minChars ?? 0;
  const hasValue = value !== null && value !== undefined && value !== '';

  // Etiqueta del valor actual (modo edición): se resuelve solo si hace falta.
  useEffect(() => {
    if (!hasValue) { setLabel(''); if (!open) setText(''); return; }
    if (valueLabel) { setLabel(valueLabel); if (!open) setText(valueLabel); return; }
    let cancel = false;
    source?.resolveLabel?.(value).then((l) => {
      if (!cancel && l) { setLabel(l); if (!open) setText(l); }
    }).catch(() => {});
    return () => { cancel = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, valueLabel]);

  const buscar = useCallback((q) => {
    if (q.trim().length < minChars) { setOptions([]); setLoading(false); return; }
    const id = ++reqId.current;
    setLoading(true); setError('');
    source.loadOptions(q.trim())
      .then((opts) => { if (id === reqId.current) { setOptions(opts); setActive(opts.length ? 0 : -1); } })
      .catch(() => { if (id === reqId.current) setError('No se pudieron cargar las opciones'); })
      .finally(() => { if (id === reqId.current) setLoading(false); });
  }, [source, minChars]);

  // Debounce de la búsqueda mientras el menú está abierto.
  useEffect(() => {
    if (!open) return;
    const q = text === label ? '' : text;   // al abrir con un valor ya elegido, mostrar opciones generales
    const t = setTimeout(() => buscar(q), debounce);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text, open]);

  // Cerrar al hacer clic fuera y restaurar el texto del valor elegido.
  useEffect(() => {
    const h = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) { setOpen(false); setText(label); }
    };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, [label]);

  const elegir = (opt) => {
    setLabel(opt.label); setText(opt.label); setOpen(false);
    onChange?.(opt.value, opt);
  };
  const limpiar = () => {
    setLabel(''); setText(''); setOptions([]);
    onChange?.('', null);
  };

  const onKeyDown = (e) => {
    if (!open && (e.key === 'ArrowDown' || e.key === 'Enter')) { setOpen(true); e.preventDefault(); return; }
    if (e.key === 'ArrowDown') { setActive((a) => Math.min(a + 1, options.length - 1)); e.preventDefault(); }
    else if (e.key === 'ArrowUp') { setActive((a) => Math.max(a - 1, 0)); e.preventDefault(); }
    else if (e.key === 'Enter') { if (open && options[active]) { elegir(options[active]); e.preventDefault(); } }
    else if (e.key === 'Escape') { setOpen(false); setText(label); }
  };

  const faltanLetras = text !== label && text.trim().length < minChars;

  return (
    <div ref={boxRef} className={`relative ${className}`}>
      <div className="relative">
        <input
          type="text"
          className="input-field pr-14"
          value={text}
          placeholder={placeholder}
          disabled={disabled}
          required={required && !hasValue}
          autoComplete="off"
          role="combobox"
          aria-expanded={open}
          onChange={(e) => { setText(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
        <div className="absolute inset-y-0 right-2 flex items-center gap-1 text-gray-400">
          {clearable && hasValue && !disabled && (
            <button type="button" onClick={limpiar} className="p-1 hover:text-gray-700" aria-label="Limpiar"><FaTimes size={11} /></button>
          )}
          <FaChevronDown size={11} />
        </div>
      </div>

      {open && !disabled && (
        <div className="absolute z-30 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto" role="listbox">
          {faltanLetras && <div className="px-3 py-2 text-sm text-gray-400">Escribe al menos {minChars} letras…</div>}
          {!faltanLetras && loading && <div className="px-3 py-2 text-sm text-gray-400">Buscando…</div>}
          {!faltanLetras && !loading && error && <div className="px-3 py-2 text-sm text-red-600">{error}</div>}
          {!faltanLetras && !loading && !error && options.length === 0 && <div className="px-3 py-2 text-sm text-gray-400">Sin resultados</div>}
          {!faltanLetras && !loading && options.map((o, i) => (
            <button
              type="button" key={o.value} role="option" aria-selected={String(o.value) === String(value)}
              onMouseDown={(e) => { e.preventDefault(); elegir(o); }}
              onMouseEnter={() => setActive(i)}
              className={`w-full text-left px-3 py-2 text-sm flex justify-between gap-2 ${i === active ? 'bg-primary-50 text-primary-700' : ''} ${String(o.value) === String(value) ? 'font-semibold' : ''}`}
            >
              <span className="truncate">{o.label}</span>
              {o.sublabel && <span className="text-xs text-gray-400 truncate">{o.sublabel}</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
