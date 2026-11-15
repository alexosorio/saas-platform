import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, Legend,
  AreaChart, Area, Cell,
} from 'recharts';
import {
  FaMoneyBillWave, FaHandHoldingUsd, FaExclamationTriangle, FaChartLine, FaBoxOpen, FaSyncAlt,
} from 'react-icons/fa';
import dashboardService from '../../services/dashboardService';
import { brand } from '@saas/ui-shell';
import '../../index.css';

const fmt = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const money = (n) => fmt.format(Number(n || 0));
const compact = (n) => new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 }).format(Number(n || 0));
const diaCorto = (iso) => { const [, m, d] = String(iso).split('-'); return `${d}/${m}`; };
const hace = (iso) => {
  if (!iso) return '';
  const dias = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
  return dias <= 0 ? 'hoy' : dias === 1 ? 'ayer' : `hace ${dias} días`;
};
// Colores por antigüedad: al día → crítico
const COLOR_RANGO = ['#10b981', '#fbbf24', '#f97316', '#ef4444', '#991b1b'];

const Kpi = ({ icon, label, value, sub, tone = 'text-gray-800' }) => (
  <div className="card">
    <div className="flex items-center gap-2 text-gray-500 text-xs font-medium uppercase tracking-wide">{icon}{label}</div>
    <div className={`text-2xl font-bold mt-1 ${tone}`}>{value}</div>
    {sub && <div className="text-xs text-gray-500 mt-1">{sub}</div>}
  </div>
);

const Panel = ({ title, icon, right, children }) => (
  <div className="card">
    <div className="flex justify-between items-center mb-3">
      <h3 className="font-semibold flex items-center gap-2">{icon}{title}</h3>
      {right}
    </div>
    {children}
  </div>
);

const Vacio = ({ children }) => <div className="h-full min-h-[120px] flex items-center justify-center text-sm text-gray-400">{children}</div>;

export default function Dashboard() {
  const [dias, setDias] = useState(30);
  const [umbral, setUmbral] = useState(null);   // null = el configurado por la organización
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const cargar = async () => {
    setLoading(true); setError('');
    try { setData(await dashboardService.resumen({ dias, umbral })); }
    catch (e) { setError(e.response?.data?.message || 'No se pudo cargar el tablero'); }
    finally { setLoading(false); }
  };
  useEffect(() => { cargar(); /* eslint-disable-next-line */ }, [dias, umbral]);

  if (loading && !data) return <div className="text-center py-10 text-gray-500">Cargando tablero…</div>;
  if (error && !data) return <div className="bg-red-100 text-red-700 px-4 py-3 rounded">{error}</div>;
  const t = data.totales;

  // Cartera por edades: por pagar y por cobrar lado a lado
  const cartera = data.carteraPorPagar.map((r, i) => ({
    rango: r.rango,
    porPagar: Number(r.saldo), cuentasPagar: r.cuentas,
    porCobrar: Number(data.carteraPorCobrar[i]?.saldo || 0), cuentasCobrar: data.carteraPorCobrar[i]?.cuentas || 0,
  }));
  const proveedores = data.porPagarPorProveedor.slice(0, 8).map(p => ({ ...p, saldo: Number(p.saldo) }));
  const ingresos = data.ingresosPorDia.map(d => ({ fecha: d.fecha, monto: Number(d.monto) }));
  const hayIngresos = ingresos.some(d => d.monto > 0);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap justify-between items-center gap-3">
        <h2 className="text-xl font-bold">Resumen del negocio</h2>
        <div className="flex items-center gap-2 text-sm">
          <span className="text-gray-500">Ingresos de los últimos</span>
          {[7, 30, 90].map(d => (
            <button key={d} onClick={() => setDias(d)}
              className={`px-2.5 py-1 rounded ${dias === d ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700'}`}>{d} días</button>
          ))}
          <button onClick={cargar} className="ml-1 p-2 rounded hover:bg-gray-100 text-gray-500" title="Actualizar">
            <FaSyncAlt className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Kpi icon={<FaMoneyBillWave />} label="Por pagar" value={money(t.porPagar)}
          sub={<span className={Number(t.vencidoPorPagar) > 0 ? 'text-red-600' : ''}>Vencido: {money(t.vencidoPorPagar)}</span>} />
        <Kpi icon={<FaHandHoldingUsd />} label="Por cobrar" value={money(t.porCobrar)}
          sub={<span className={Number(t.vencidoPorCobrar) > 0 ? 'text-red-600' : ''}>Vencido: {money(t.vencidoPorCobrar)}</span>} />
        <Kpi icon={<FaChartLine />} label={`Ingresos ${t.diasPeriodo} días`} value={money(t.ingresosPeriodo)}
          sub="Pagos recibidos de órdenes" tone="text-primary-700" />
        <Kpi icon={<FaExclamationTriangle />} label="Stock bajo" value={data.stockBajo.length}
          sub={`Productos con ≤ ${t.umbralStock} disponibles`} tone={data.stockBajo.length ? 'text-amber-600' : 'text-gray-800'} />
      </div>

      {/* Ingresos por día */}
      <Panel title="Ingresos por día" icon={<FaChartLine className="text-primary-600" />}>
        <div className="h-64">
          {hayIngresos ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={ingresos} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="gIngreso" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={brand.principal} stopOpacity={0.35} />
                    <stop offset="95%" stopColor={brand.principal} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="fecha" tickFormatter={diaCorto} tick={{ fontSize: 11 }} minTickGap={16} />
                <YAxis tickFormatter={compact} tick={{ fontSize: 11 }} width={48} />
                <Tooltip formatter={(v) => [money(v), 'Ingresos']} labelFormatter={diaCorto} />
                <Area type="monotone" dataKey="monto" stroke={brand.principal} strokeWidth={2} fill="url(#gIngreso)" />
              </AreaChart>
            </ResponsiveContainer>
          ) : <Vacio>No se registraron pagos en los últimos {t.diasPeriodo} días.</Vacio>}
        </div>
      </Panel>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Cuentas por pagar por proveedor */}
        <Panel title="Por pagar por proveedor" icon={<FaMoneyBillWave className="text-red-600" />}
          right={<Link to="/catalog/cuentas-por-pagar" className="text-xs text-primary-600">Ver cuentas</Link>}>
          <div className="h-72">
            {proveedores.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={proveedores} layout="vertical" margin={{ top: 0, right: 16, left: 8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                  <XAxis type="number" tickFormatter={compact} tick={{ fontSize: 11 }} />
                  <YAxis type="category" dataKey="proveedor" width={110} tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(v, _n, p) => [`${money(v)} · ${p.payload.cuentas} cuenta(s)`, 'Saldo']} />
                  <Bar dataKey="saldo" fill="#ef4444" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : <Vacio>No hay saldos pendientes con proveedores.</Vacio>}
          </div>
          {data.porPagarPorProveedor.length > 8 &&
            <p className="text-xs text-gray-400 mt-1">Mostrando los 8 proveedores con mayor saldo.</p>}
        </Panel>

        {/* Cartera vencida por edades */}
        <Panel title="Cartera por antigüedad de vencimiento" icon={<FaExclamationTriangle className="text-amber-500" />}>
          <div className="h-72">
            {cartera.some(r => r.porPagar || r.porCobrar) ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={cartera} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="rango" tick={{ fontSize: 11 }} />
                  <YAxis tickFormatter={compact} tick={{ fontSize: 11 }} width={48} />
                  <Tooltip formatter={(v, n, p) => [
                    `${money(v)} · ${n === 'Por pagar' ? p.payload.cuentasPagar : p.payload.cuentasCobrar} cuenta(s)`, n]} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="porPagar" name="Por pagar" radius={[4, 4, 0, 0]}>
                    {cartera.map((_, i) => <Cell key={i} fill={COLOR_RANGO[i]} />)}
                  </Bar>
                  <Bar dataKey="porCobrar" name="Por cobrar" fill={brand.acento} radius={[4, 4, 0, 0]} fillOpacity={0.85} />
                </BarChart>
              </ResponsiveContainer>
            ) : <Vacio>No hay cuentas abiertas.</Vacio>}
          </div>
          <p className="text-xs text-gray-400 mt-1">Días transcurridos desde la fecha de vencimiento de cada cuenta con saldo.</p>
        </Panel>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Recién llegados */}
        <Panel title="Recién llegados a stock" icon={<FaBoxOpen className="text-primary-600" />}
          right={<Link to="/catalog/inventory" className="text-xs text-primary-600">Ver inventario</Link>}>
          {data.recienLlegados.length ? (
            <div className="divide-y">
              {data.recienLlegados.map(u => (
                <div key={u.unidadId} className="py-2 flex justify-between gap-3 text-sm">
                  <div className="min-w-0">
                    <div className="font-medium truncate">{u.producto}</div>
                    <div className="text-xs text-gray-500 truncate">
                      <span className="font-mono">{u.codigo}</span>
                      {u.bodega && <> · {u.bodega}</>}{u.proveedor && <> · {u.proveedor}</>}
                    </div>
                  </div>
                  <span className="text-xs text-gray-400 whitespace-nowrap">{hace(u.fecha)}</span>
                </div>
              ))}
            </div>
          ) : <Vacio>Aún no hay unidades disponibles.</Vacio>}
        </Panel>

        {/* Stock bajo */}
        <Panel title="Stock bajo" icon={<FaExclamationTriangle className="text-amber-500" />}
          right={
            <label className="text-xs text-gray-500 flex items-center gap-1">Umbral ≤
              <select value={umbral ?? data.totales.umbralStock} onChange={e => setUmbral(Number(e.target.value))} className="input-field input-sm w-auto">
                {[...new Set([0, 1, 3, 5, 10, 20, data.totales.umbralStock])].sort((a, b) => a - b).map(n => <option key={n} value={n}>{n}</option>)}
              </select>
            </label>
          }>
          {data.stockBajo.length ? (
            <div className="divide-y">
              {data.stockBajo.map(p => (
                <div key={p.productoId} className="py-2 flex justify-between items-center gap-3 text-sm">
                  <Link to={`/catalog/products/${p.productoId}`} className="truncate hover:text-primary-700">{p.producto}</Link>
                  <span className={`text-xs font-semibold px-2 py-0.5 rounded whitespace-nowrap ${
                    p.disponible === 0 ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                    {p.disponible === 0 ? 'Agotado' : `${p.disponible} disp.`}
                  </span>
                </div>
              ))}
            </div>
          ) : <Vacio>Ningún producto por debajo del umbral.</Vacio>}
        </Panel>
      </div>
    </div>
  );
}
