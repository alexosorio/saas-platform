import { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { FaArrowLeft, FaSave, FaTrash, FaKey, FaExclamationTriangle } from 'react-icons/fa';
import { Autocomplete, lazyOptions } from '@saas/ui-shell';
import roleService from '../../services/roleService';

// Presets: Lectura = leer · Escritura = crear+actualizar+eliminar · Ambas = todo
const PRESETS = {
  lectura:   { leer: true,  crear: false, actualizar: false, eliminar: false },
  escritura: { leer: false, crear: true,  actualizar: true,  eliminar: true  },
  ambas:     { leer: true,  crear: true,  actualizar: true,  eliminar: true  },
};
const presetDe = (p) => {
  const w = p.crear && p.actualizar && p.eliminar;
  if (p.leer && w) return 'ambas';
  if (p.leer && !p.crear && !p.actualizar && !p.eliminar) return 'lectura';
  if (!p.leer && w) return 'escritura';
  return 'personalizado';
};
const ACCIONES = [['leer', 'Leer'], ['crear', 'Crear'], ['actualizar', 'Actualizar'], ['eliminar', 'Eliminar']];

const RolePermissions = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [rol, setRol] = useState(null);
  const [permisos, setPermisos] = useState([]);   // [{moduloId, moduloNombre, moduloCodigo, leer, crear, actualizar, eliminar}]
  const [inicial, setInicial] = useState('[]');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [addKey, setAddKey] = useState(0);       // remonta el autocomplete tras agregar

  useEffect(() => { (async () => {
    try {
      const r = await roleService.getRoleById(id);
      setRol(r);
      const ps = (r.permisos || []).map(p => ({
        moduloId: p.moduloId, moduloNombre: p.moduloNombre, moduloCodigo: p.moduloCodigo,
        leer: !!p.leer, crear: !!p.crear, actualizar: !!p.actualizar, eliminar: !!p.eliminar,
      }));
      setPermisos(ps); setInicial(JSON.stringify(ps));
    } catch (e) { setError(e.response?.data?.message || 'No se pudo cargar el rol'); }
    finally { setLoading(false); }
  })(); }, [id]);

  // Catálogo de módulos bajo demanda; el autocomplete excluye los ya asignados
  const baseModulos = useMemo(() => lazyOptions(() => roleService.getModulos(), { sublabel: 'codigo' }), []);
  const asignados = useMemo(() => new Set(permisos.map(p => String(p.moduloId))), [permisos]);
  const moduloSrc = useMemo(() => ({
    minChars: 0,
    loadOptions: async (q) => (await baseModulos.loadOptions(q)).filter(o => !asignados.has(String(o.value))),
  }), [baseModulos, asignados]);

  const agregar = (v, o) => {
    if (!v || asignados.has(String(v))) return;
    setPermisos(ps => [...ps, { moduloId: v, moduloNombre: o.raw.nombre, moduloCodigo: o.raw.codigo, ...PRESETS.lectura }]);
    setAddKey(k => k + 1); setOk('');
  };
  const quitar = (moduloId) => { setPermisos(ps => ps.filter(p => p.moduloId !== moduloId)); setOk(''); };
  const aplicarPreset = (moduloId, preset) => {
    setPermisos(ps => ps.map(p => p.moduloId === moduloId ? { ...p, ...PRESETS[preset] } : p)); setOk('');
  };
  const toggle = (moduloId, accion) => {
    setPermisos(ps => ps.map(p => p.moduloId === moduloId ? { ...p, [accion]: !p[accion] } : p)); setOk('');
  };

  const editable = rol?.editable !== false;
  const sucio = JSON.stringify(permisos) !== inicial;
  const vacios = permisos.filter(p => !p.leer && !p.crear && !p.actualizar && !p.eliminar);

  const guardar = async () => {
    setError(''); setOk(''); setSaving(true);
    try {
      const payload = permisos.map(({ moduloId, leer, crear, actualizar, eliminar }) => ({ moduloId, leer, crear, actualizar, eliminar }));
      const r = await roleService.updatePermisos(id, payload);
      const ps = (r.permisos || []).map(p => ({
        moduloId: p.moduloId, moduloNombre: p.moduloNombre, moduloCodigo: p.moduloCodigo,
        leer: !!p.leer, crear: !!p.crear, actualizar: !!p.actualizar, eliminar: !!p.eliminar,
      }));
      setPermisos(ps); setInicial(JSON.stringify(ps));
      setOk('Permisos guardados. Los usuarios con este rol los verán al renovar su sesión.');
    } catch (e) { setError(e.response?.data?.message || 'No se pudieron guardar los permisos'); }
    finally { setSaving(false); }
  };

  if (loading) return <div className="text-center py-10 text-gray-500">Cargando...</div>;

  return (
    <div className="max-w-5xl mx-auto">
      <button onClick={() => navigate('/roles')} className="text-gray-500 mb-4 flex items-center gap-2"><FaArrowLeft /> Volver a roles</button>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-3"><FaKey className="text-primary-600" /> Permisos · {rol?.nombre}</h1>
        {editable && <button onClick={guardar} disabled={!sucio || saving} className="btn-primary flex items-center gap-2 disabled:opacity-50">
          <FaSave /> {saving ? 'Guardando...' : 'Guardar cambios'}
        </button>}
      </div>

      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      {!editable && (
        <div className="bg-gray-50 border border-gray-200 text-gray-700 px-4 py-3 rounded mb-4 text-sm flex flex-wrap items-center gap-3">
          <span>Este es un <strong>rol del sistema</strong>: lo compartes con otras organizaciones y no se puede modificar.
            Para adaptarlo, crea una copia editable en tu organización.</span>
          <button onClick={async () => {
              try { const c = await roleService.personalizar(id); navigate(`/roles/${c.id}/permisos`); window.location.reload(); }
              catch (e) { setError(e.response?.data?.message || 'No se pudo personalizar'); }
            }} className="btn-primary text-sm">Personalizar</button>
        </div>
      )}
      {ok && <div className="bg-green-100 text-green-700 px-4 py-3 rounded mb-4">{ok}</div>}
      {permisos.some(p => p.moduloCodigo === '*') && (
        <div className="bg-amber-50 border border-amber-300 text-amber-800 px-4 py-3 rounded mb-4 flex items-center gap-2 text-sm">
          <FaExclamationTriangle /> Este rol tiene el módulo comodín <b>*</b>: sus acciones aplican a <b>todos</b> los módulos.
        </div>
      )}

      {editable && <div className="card mb-4">
        <label className="block font-semibold mb-2">Agregar módulo</label>
        <div className="max-w-md">
          <Autocomplete key={addKey} source={moduloSrc} value="" onChange={agregar}
            placeholder="Buscar módulo para asignar…" clearable={false} />
        </div>
        <p className="text-xs text-gray-400 mt-2">Se agrega con permiso de lectura; ajústalo en la tabla.</p>
      </div>}

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead><tr className="text-left text-gray-500 border-b">
            <th className="py-2 px-3">Módulo</th>
            <th className="py-2 px-3">Acceso</th>
            {ACCIONES.map(([k, l]) => <th key={k} className="py-2 px-2 text-center">{l}</th>)}
            <th className="py-2 px-3"></th>
          </tr></thead>
          <tbody>
            {permisos.map(p => {
              const preset = presetDe(p);
              return (
                <tr key={p.moduloId} className="border-b hover:bg-gray-50">
                  <td className="py-2 px-3">
                    <div className="font-medium">{p.moduloNombre}</div>
                    <div className="text-xs text-gray-400 font-mono">{p.moduloCodigo}</div>
                  </td>
                  <td className="py-2 px-3">
                    <div className="inline-flex rounded-lg border border-gray-200 overflow-hidden">
                      {[['lectura', 'Lectura'], ['escritura', 'Escritura'], ['ambas', 'Ambas']].map(([k, l]) => (
                        <button key={k} type="button" disabled={!editable} onClick={() => aplicarPreset(p.moduloId, k)}
                          className={`px-3 py-1 text-xs ${preset === k ? 'bg-primary-600 text-white' : 'bg-white text-gray-600 hover:bg-gray-50'}`}>
                          {l}
                        </button>
                      ))}
                    </div>
                    {preset === 'personalizado' && <span className="ml-2 text-xs text-gray-400">personalizado</span>}
                  </td>
                  {ACCIONES.map(([k]) => (
                    <td key={k} className="py-2 px-2 text-center">
                      <input type="checkbox" checked={p[k]} disabled={!editable} onChange={() => toggle(p.moduloId, k)} className="w-4 h-4" />
                    </td>
                  ))}
                  <td className="py-2 px-3 text-right">
                    {editable && <button onClick={() => quitar(p.moduloId)} className="text-red-600" title="Quitar módulo"><FaTrash /></button>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {permisos.length === 0 && <div className="text-center py-8 text-gray-500">Este rol no tiene módulos asignados.</div>}
      </div>

      {vacios.length > 0 && (
        <p className="text-xs text-amber-700 mt-2">
          {vacios.length} módulo(s) sin ninguna acción marcada no se guardarán (equivale a quitarlos).
        </p>
      )}
      <p className="text-xs text-gray-400 mt-2">
        Escritura = crear, actualizar y eliminar. ¿Buscas otro rol? <Link to="/roles" className="underline">Ver roles</Link>
      </p>
    </div>
  );
};

export default RolePermissions;
