import { useState, useEffect } from 'react';
import { Paginacion, usePaginacionServidor } from '@saas/ui-shell';
import { FaHistory, FaSearch } from 'react-icons/fa';
import api from '../../services/api';

const ACCIONES = {
  ORDEN_CREADA: 'Orden creada', ORDEN_RESERVADA: 'Orden reservada', ORDEN_CONFIRMADA: 'Orden confirmada',
  ORDEN_REVERSADA: 'Orden reversada', ORDEN_CANCELADA: 'Orden cancelada', PAGO_ORDEN_REGISTRADO: 'Pago de orden',
  PAGO_PROVEEDOR_REGISTRADO: 'Pago a proveedor', INVENTARIO_INGRESO: 'Ingreso a inventario', INVENTARIO_MOVIMIENTO: 'Movimiento de inventario',
  PAGO_SUSCRIPCION_REGISTRADO: 'Pago de suscripción', PAGO_SUSCRIPCION_APROBADO: 'Pago de suscripción aprobado',
  PAGO_SUSCRIPCION_RECHAZADO: 'Pago de suscripción rechazado', SUSCRIPCION_ACTIVADA: 'Suscripción activada',
  SUSCRIPCION_SUSPENDIDA: 'Suscripción suspendida', SUSCRIPCION_SUSPENDIDA_AUTOMATICA: 'Suspensión automática',
  USUARIO_CREADO: 'Usuario creado', USUARIO_ACTUALIZADO: 'Usuario actualizado', USUARIO_DESACTIVADO: 'Usuario desactivado',
  USUARIO_ACTIVADO: 'Usuario activado', USUARIO_ELIMINADO: 'Usuario eliminado', ROL_CREADO: 'Rol creado',
  ROL_PERMISOS_ACTUALIZADOS: 'Permisos de rol', ROL_PERSONALIZADO: 'Rol personalizado', ROL_ELIMINADO: 'Rol eliminado',
  BODEGAS_ASIGNADAS: 'Bodegas asignadas', PARAMETRO_ACTUALIZADO: 'Parámetro', PARAMETRO_GENERAL_ACTUALIZADO: 'Parámetro general',
  PARAMETRO_RESTABLECIDO: 'Parámetro restablecido', ORGANIZACION_REGISTRADA: 'Organización registrada',
  UNIDAD_DADA_DE_BAJA: 'Unidad dada de baja', DEVOLUCION_VENTA: 'Devolución de venta', ASIENTO_CONTABLE: 'Asiento contable', NOTA_CREDITO_CLIENTE: 'Nota crédito a cliente', NOTA_CREDITO_CXP: 'Nota crédito a proveedor',
  BODEGA_ACTIVADA: 'Bodega activada', BODEGA_DESACTIVADA: 'Bodega desactivada', SEDE_ACTIVADA: 'Sede activada', SEDE_DESACTIVADA: 'Sede desactivada',
};
const fechaHora = (v) => v ? new Date(v.replace(' ', 'T')).toLocaleString('es-CO') : '';
const detalleLegible = (d) => { try { return Object.entries(JSON.parse(d)).map(([k, v]) => `${k}: ${v}`).join(' · '); } catch { return d || ''; } };

export default function Auditoria() {
  const [f, setF] = useState({ desde: '', hasta: '', accion: '', usuario: '' });
  // El filtro de usuario espera a que se deje de escribir
  const [usuario, setUsuario] = useState('');
  useEffect(() => { const t = setTimeout(() => setUsuario(f.usuario.trim()), 400); return () => clearTimeout(t); }, [f.usuario]);

  // Solo se trae la página visible
  const pag = usePaginacionServidor(async ({ page, size }) => {
    const params = Object.fromEntries(Object.entries({ desde: f.desde, hasta: f.hasta, accion: f.accion, usuario }).filter(([, v]) => v));
    try { return (await api.get('/api/auditoria/paginado', { params: { ...params, page, size } })).data; }
    catch (e) { throw e.response?.status === 403 ? { response: { data: { message: 'No tienes permiso para consultar la auditoría.' } } } : e; }
  }, [f.desde, f.hasta, f.accion, usuario], { clave: 'auditoria', tamano: 50 });
  const items = pag.items;
  const loading = pag.cargando && items.length === 0;
  const error = pag.error;

  return (
    <div>
      <h1 className="text-3xl font-bold flex items-center gap-3 mb-2"><FaHistory className="text-primary-600" /> Auditoría</h1>
      <p className="text-sm text-gray-500 mb-4">Registro inalterable de las operaciones críticas: no se puede editar ni borrar. Del más reciente al más antiguo.</p>
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}
      <div className="card mb-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <div><label className="block text-xs text-gray-500 mb-1">Desde</label><input type="date" className="input-field" value={f.desde} onChange={e => setF({ ...f, desde: e.target.value })} /></div>
        <div><label className="block text-xs text-gray-500 mb-1">Hasta</label><input type="date" className="input-field" value={f.hasta} onChange={e => setF({ ...f, hasta: e.target.value })} /></div>
        <div><label className="block text-xs text-gray-500 mb-1">Acción</label>
          <select className="input-field" value={f.accion} onChange={e => setF({ ...f, accion: e.target.value })}>
            <option value="">Todas</option>
            {Object.entries(ACCIONES).sort((a, b) => a[1].localeCompare(b[1])).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div><label className="block text-xs text-gray-500 mb-1">Usuario</label>
          <div className="relative"><FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input className="input-field pl-9" placeholder="Correo…" value={f.usuario} onChange={e => setF({ ...f, usuario: e.target.value })} /></div></div>
      </div>
      <div className="card overflow-x-auto">
        {loading ? <div className="py-8 text-center text-gray-500">Cargando…</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b">
              <th className="py-2 px-3">Fecha</th><th className="py-2 px-3">Usuario</th><th className="py-2 px-3">Acción</th>
              <th className="py-2 px-3">Registro</th><th className="py-2 px-3">Detalle</th><th className="py-2 px-3">IP</th>
            </tr></thead>
            <tbody>
              {items.map(a => (
                <tr key={a.id} className="border-b align-top hover:bg-gray-50">
                  <td className="py-2 px-3 whitespace-nowrap text-xs">{fechaHora(a.fecha)}</td>
                  <td className="py-2 px-3 text-xs">{a.usuario || <span className="text-gray-400">Sistema</span>}
                    {a.organizacion && <div className="text-gray-400">{a.organizacion}</div>}</td>
                  <td className="py-2 px-3"><span className="text-xs bg-primary-50 text-primary-700 px-2 py-0.5 rounded whitespace-nowrap">{ACCIONES[a.accion] || a.accion}</span></td>
                  <td className="py-2 px-3 text-xs text-gray-600 whitespace-nowrap">{a.entidad} #{a.entidadId}</td>
                  <td className="py-2 px-3 text-xs text-gray-600 max-w-md">{detalleLegible(a.detalle)}</td>
                  <td className="py-2 px-3 text-xs text-gray-400 font-mono">{a.ip}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && items.length === 0 && <div className="py-8 text-center text-gray-500">No hay registros con esos filtros.</div>}
        <Paginacion {...pag.paginador} />
      </div>
    </div>
  );
}
