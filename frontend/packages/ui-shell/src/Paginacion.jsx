import { useState, useEffect, useMemo, useCallback } from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';

const TAMANOS = [10, 20, 50, 100];

/** Números de página a mostrar: 1 … 4 5 [6] 7 8 … 20 */
function paginasVisibles(actual, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const set = new Set([0, total - 1, actual - 1, actual, actual + 1]);
  if (actual <= 2) [1, 2, 3].forEach((i) => set.add(i));
  if (actual >= total - 3) [total - 4, total - 3, total - 2].forEach((i) => set.add(i));
  const orden = [...set].filter((i) => i >= 0 && i < total).sort((a, b) => a - b);
  const out = [];
  orden.forEach((i, k) => { if (k > 0 && i - orden[k - 1] > 1) out.push('…' + i); out.push(i); });
  return out;
}

/**
 * Barra de paginación (páginas base 0).
 *   <Paginacion pagina={0} totalPaginas={8} total={153} tamano={20} onPagina={…} onTamano={…} />
 * Lo normal es usarla con los hooks: <Paginacion {...p.paginador} />
 */
export default function Paginacion({
  pagina = 0, totalPaginas = 0, total = 0, tamano = 20, onPagina, onTamano,
  cargando = false, tamanos = TAMANOS, className = '',
}) {
  if (!total) return null;
  const desde = pagina * tamano + 1;
  const hasta = Math.min(total, (pagina + 1) * tamano);
  const ir = (p) => { if (!cargando && p >= 0 && p < totalPaginas && p !== pagina) onPagina?.(p); };
  const btn = 'min-w-[2rem] h-8 px-2 rounded-md text-sm flex items-center justify-center transition-colors';

  return (
    <div className={`flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-gray-100 text-sm text-gray-600 ${className}`}>
      <div className="flex items-center gap-3">
        <span>
          {desde.toLocaleString('es-CO')}–{hasta.toLocaleString('es-CO')} de <strong className="text-gray-800">{total.toLocaleString('es-CO')}</strong>
        </span>
        {onTamano && (
          <label className="flex items-center gap-1.5 text-gray-500">
            <span className="hidden sm:inline">Por página</span>
            <select value={tamano} onChange={(e) => onTamano(Number(e.target.value))} disabled={cargando}
              className="border border-gray-200 rounded-md py-1 pl-2 pr-6 text-sm bg-white">
              {tamanos.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
        )}
      </div>

      {totalPaginas > 1 && (
        <nav className="flex items-center gap-1" aria-label="Paginación">
          <button type="button" onClick={() => ir(pagina - 1)} disabled={pagina === 0 || cargando}
            className={`${btn} text-gray-600 hover:bg-gray-100 disabled:opacity-40 disabled:hover:bg-transparent`} aria-label="Anterior">
            <FaChevronLeft size={11} />
          </button>
          {paginasVisibles(pagina, totalPaginas).map((p) => (typeof p === 'string'
            ? <span key={p} className="px-1 text-gray-400">…</span>
            : <button type="button" key={p} onClick={() => ir(p)} disabled={cargando}
                aria-current={p === pagina ? 'page' : undefined}
                className={`${btn} ${p === pagina ? 'bg-primary-600 text-white font-medium' : 'text-gray-700 hover:bg-gray-100'}`}>
                {p + 1}
              </button>))}
          <button type="button" onClick={() => ir(pagina + 1)} disabled={pagina >= totalPaginas - 1 || cargando}
            className={`${btn} text-gray-600 hover:bg-gray-100 disabled:opacity-40 disabled:hover:bg-transparent`} aria-label="Siguiente">
            <FaChevronRight size={11} />
          </button>
        </nav>
      )}
    </div>
  );
}

const leerTamano = (clave, porDefecto) => {
  if (!clave) return porDefecto;
  try { const v = Number(localStorage.getItem('pag:' + clave)); return TAMANOS.includes(v) ? v : porDefecto; }
  catch { return porDefecto; }
};
const guardarTamano = (clave, t) => { if (clave) try { localStorage.setItem('pag:' + clave, String(t)); } catch { /* sin almacenamiento */ } };

/**
 * Paginación en el NAVEGADOR para listas que ya están completas en memoria (catálogos pequeños).
 *   const p = usePaginacionLocal(items, { clave: 'marcas', reiniciar: busqueda });
 *   p.visibles.map(...)   <Paginacion {...p.paginador} />
 * reiniciar: cuando cambia (p. ej. el texto de búsqueda) se vuelve a la página 1.
 */
export function usePaginacionLocal(items, { tamano: t0 = 20, clave, reiniciar } = {}) {
  const lista = items || [];
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamanoBase] = useState(() => leerTamano(clave, t0));
  const totalPaginas = Math.max(1, Math.ceil(lista.length / tamano));

  useEffect(() => { setPagina(0); }, [reiniciar]);
  // Si la lista se achica (filtro, eliminación), no quedarse en una página vacía
  useEffect(() => { if (pagina > totalPaginas - 1) setPagina(totalPaginas - 1); }, [pagina, totalPaginas]);

  const setTamano = useCallback((t) => { setTamanoBase(t); guardarTamano(clave, t); setPagina(0); }, [clave]);
  const efectiva = Math.min(pagina, totalPaginas - 1);
  const visibles = useMemo(() => lista.slice(efectiva * tamano, efectiva * tamano + tamano), [lista, efectiva, tamano]);

  return {
    visibles, pagina: efectiva, setPagina, tamano, setTamano, total: lista.length, totalPaginas,
    /** Desplazamiento de la página (para numerar filas) */
    inicio: efectiva * tamano,
    paginador: { pagina: efectiva, totalPaginas, total: lista.length, tamano, onPagina: setPagina, onTamano: setTamano },
  };
}

/** Respuesta del backend: { contenido, pagina, tamano, totalElementos, totalPaginas, resumen } (o un arreglo). */
const normalizar = (r) => (Array.isArray(r)
  ? { contenido: r, totalElementos: r.length, totalPaginas: 1, resumen: null }
  : { contenido: r?.contenido || [], totalElementos: r?.totalElementos || 0, totalPaginas: r?.totalPaginas || 0, resumen: r?.resumen || null });

/**
 * Paginación en el SERVIDOR: solo se trae la página visible.
 *   const p = usePaginacionServidor(({ page, size }) => orderService.paginado({ page, size, estado }), [estado], { clave: 'ordenes' });
 *   p.items.map(...)   p.resumen   <Paginacion {...p.paginador} />   p.recargar()
 * deps: filtros; al cambiar, vuelve a la página 1.
 */
export function usePaginacionServidor(cargar, deps = [], { tamano: t0 = 20, clave } = {}) {
  const depsKey = JSON.stringify(deps);
  const [estado, setEstado] = useState({ key: depsKey, pagina: 0 });
  const [tamano, setTamanoBase] = useState(() => leerTamano(clave, t0));
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0, totalPaginas: 0, resumen: null });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);

  // Al cambiar los filtros la página vuelve a 0 sin hacer una consulta intermedia con la página vieja
  const pagina = estado.key === depsKey ? estado.pagina : 0;
  const setPagina = useCallback((p) => setEstado({ key: depsKey, pagina: p }), [depsKey]);

  useEffect(() => {
    let vivo = true;
    setCargando(true); setError('');
    Promise.resolve(cargar({ page: pagina, size: tamano }))
      .then((r) => {
        if (!vivo) return;
        const d = normalizar(r);
        // Página que quedó vacía (p. ej. se eliminó su último registro): ir a la última con datos
        if (d.contenido.length === 0 && pagina > 0 && d.totalPaginas > 0) { setPagina(d.totalPaginas - 1); return; }
        setDatos(d);
      })
      .catch((e) => { if (vivo) setError(e?.response?.data?.message || 'No se pudo cargar la información'); })
      .finally(() => { if (vivo) setCargando(false); });
    return () => { vivo = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, tamano, depsKey, version]);

  const setTamano = useCallback((t) => { setTamanoBase(t); guardarTamano(clave, t); setEstado({ key: depsKey, pagina: 0 }); }, [clave, depsKey]);
  const recargar = useCallback(() => setVersion((v) => v + 1), []);

  return {
    items: datos.contenido, total: datos.totalElementos, totalPaginas: datos.totalPaginas, resumen: datos.resumen,
    pagina, setPagina, tamano, setTamano, cargando, error, recargar,
    inicio: pagina * tamano,
    paginador: { pagina, totalPaginas: datos.totalPaginas, total: datos.totalElementos, tamano, onPagina: setPagina, onTamano: setTamano, cargando },
  };
}
