import { useState, useEffect } from 'react';
import { FaCrown } from 'react-icons/fa';
import admin from '../../services/adminSubscriptionService';
import { AdminTabs, money } from './adminUi';

const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const mes = (ym) => { const [y, m] = ym.split('-'); return `${MESES[Number(m) - 1]} ${y}`; };

const Kpi = ({ label, value, sub }) => (
  <div className="card"><div className="text-xs text-gray-500 uppercase tracking-wide">{label}</div>
    <div className="text-2xl font-bold mt-1 text-primary-700">{value}</div>{sub && <div className="text-xs text-gray-500 mt-1">{sub}</div>}</div>
);

// Barras horizontales simples (sin dependencias de gráficas)
const Barras = ({ filas, formato }) => {
  const max = Math.max(...filas.map(f => Number(f.total)), 1);
  return (
    <div className="space-y-2">
      {filas.map(f => (
        <div key={f.etiqueta} className="text-sm">
          <div className="flex justify-between mb-0.5"><span>{formato ? formato(f.etiqueta) : f.etiqueta}</span>
            <span className="font-medium">{money(f.total)} <span className="text-xs text-gray-400">· {f.cantidad} pago(s)</span></span></div>
          <div className="h-2 bg-gray-100 rounded"><div className="h-2 rounded bg-primary-500" style={{ width: `${(Number(f.total) / max) * 100}%` }} /></div>
        </div>
      ))}
    </div>
  );
};

export default function AdminFacturacion() {
  const hoy = new Date();
  const [desde, setDesde] = useState(iso(new Date(hoy.getFullYear(), 0, 1)));
  const [hasta, setHasta] = useState(iso(hoy));
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { (async () => {
    setError('');
    try { setData(await admin.facturacion(desde, hasta)); }
    catch (e) { setError(e.response?.status === 403 ? 'No tienes permiso de administración de la plataforma.' : (e.response?.data?.message || 'Error al cargar')); }
  })(); }, [desde, hasta]);

  return (
    <div>
      <h1 className="text-3xl font-bold flex items-center gap-3 mb-4"><FaCrown className="text-primary-600" /> Administración de suscripciones</h1>
      <AdminTabs />
      {error && <div className="bg-red-100 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

      <div className="card mb-4 flex flex-wrap items-end gap-3">
        <div><label className="block text-xs text-gray-500 mb-1">Desde</label><input type="date" className="input-field" value={desde} max={hasta} onChange={e => setDesde(e.target.value)} /></div>
        <div><label className="block text-xs text-gray-500 mb-1">Hasta</label><input type="date" className="input-field" value={hasta} min={desde} onChange={e => setHasta(e.target.value)} /></div>
        <p className="text-xs text-gray-500 ml-auto max-w-sm">Solo cuenta pagos <strong>aprobados</strong>, según su fecha de pago.</p>
      </div>

      {data && (<>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
          <Kpi label="Facturado" value={money(data.totalFacturado)} sub={`${data.pagosAprobados} pago(s) aprobado(s)`} />
          <Kpi label="Ticket promedio" value={money(data.ticketPromedio)} />
          <Kpi label="Suscripciones activas" value={data.suscripcionesActivas} />
          <Kpi label="Pagos por validar" value={data.pendientesValidacion} sub={data.pendientesValidacion ? 'Revisa la pestaña de validación' : 'Al día'} />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="card"><h3 className="font-semibold mb-3">Facturación por mes</h3>
            {data.porMes.length ? <Barras filas={data.porMes} formato={mes} /> : <p className="text-sm text-gray-400">Sin pagos aprobados en el período.</p>}</div>
          <div className="card"><h3 className="font-semibold mb-3">Facturación por plan</h3>
            {data.porPlan.length ? <Barras filas={data.porPlan} /> : <p className="text-sm text-gray-400">Sin pagos aprobados en el período.</p>}</div>
        </div>
      </>)}
    </div>
  );
}
