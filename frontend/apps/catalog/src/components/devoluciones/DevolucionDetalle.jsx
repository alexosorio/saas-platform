import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FaUndo, FaArrowLeft, FaBook } from 'react-icons/fa';
import devolucionService from '../../services/devolucionService';

const money = (n) => '$' + Number(n || 0).toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
const MOTIVO = { garantia: 'Garantía / defecto', cambio: 'Cambio de producto', error_venta: 'Error en la venta', desistimiento: 'Desistimiento del cliente', otro: 'Otro' };
const DESTINO = { disponible: 'Vuelve a la venta', revision: 'A revisión / reparación', baja: 'Inservible (dada de baja)' };

export default function DevolucionDetalle() {
  const { id } = useParams();
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { devolucionService.detalle(id).then(setD).catch(e => setError(e.response?.data?.message || 'No se pudo cargar')); }, [id]);

  if (error) return <div className="bg-red-100 text-red-700 px-4 py-3 rounded">{error}</div>;
  if (!d) return <div className="text-center py-10 text-gray-500">Cargando…</div>;
  const totalDebe = d.asiento.reduce((s, m) => s + Number(m.debe), 0);
  const totalHaber = d.asiento.reduce((s, m) => s + Number(m.haber), 0);

  return (
    <div className="max-w-5xl">
      <Link to="/catalog/devoluciones" className="text-gray-500 mb-4 inline-flex items-center gap-2"><FaArrowLeft /> Devoluciones</Link>
      <h1 className="text-3xl font-bold flex items-center gap-3 mb-1"><FaUndo className="text-primary-600" /> {d.numero}</h1>
      <p className="text-sm text-gray-500 mb-5">
        Factura <Link to={`/catalog/orders/${d.ordenId}`} className="text-primary-700">{d.ordenNumero}</Link>
        {' · '}{d.clienteNombre || 'Cliente ocasional'} · {String(d.createdAt || '').replace('T', ' ').slice(0, 16)}
      </p>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
        <div className="card"><div className="text-xs text-gray-500 uppercase">Valor devuelto</div><div className="text-2xl font-bold text-primary-700">{money(d.montoTotal)}</div></div>
        <div className="card"><div className="text-xs text-gray-500 uppercase">Abonado a la deuda</div><div className="text-2xl font-bold">{money(d.montoAplicadoSaldo)}</div></div>
        <div className="card"><div className="text-xs text-gray-500 uppercase">Nota crédito</div><div className="text-2xl font-bold">{money(d.montoNotaCredito)}</div>
          {Number(d.montoNotaCredito) > 0 && <div className="text-xs text-gray-500 mt-1">{d.tratamiento === 'reembolso'
            ? `Reembolso (${d.metodoReembolso || ''}${d.referenciaReembolso ? ' · ' + d.referenciaReembolso : ''})` : 'Saldo a favor del cliente'}</div>}</div>
      </div>

      <div className="card mb-4">
        <h2 className="font-semibold mb-2">Motivo: {MOTIVO[d.tipoMotivo] || d.tipoMotivo}</h2>
        <p className="text-sm text-gray-700">{d.observacion}</p>
      </div>

      <div className="card mb-4 overflow-x-auto">
        <h2 className="font-semibold mb-3">Unidades devueltas (reingreso al inventario)</h2>
        <table className="w-full text-sm">
          <thead><tr className="text-left text-gray-500 border-b"><th className="py-2 px-3">Producto</th><th className="py-2 px-3">Unidad</th>
            <th className="py-2 px-3 text-center">Cant.</th><th className="py-2 px-3">Destino</th><th className="py-2 px-3 text-right">Valor</th></tr></thead>
          <tbody>{d.lineas.map((l, i) => (
            <tr key={i} className="border-b"><td className="py-2 px-3">{l.productoNombre}</td><td className="py-2 px-3 font-mono text-xs">{l.unidadCodigo}</td>
              <td className="py-2 px-3 text-center">{l.cantidad}</td><td className="py-2 px-3">{DESTINO[l.destino] || l.destino}</td>
              <td className="py-2 px-3 text-right font-mono">{money(l.monto)}</td></tr>))}
          </tbody>
        </table>
      </div>

      <div className="card overflow-x-auto">
        <h2 className="font-semibold mb-3 flex items-center gap-2"><FaBook className="text-primary-600" /> Asiento contable</h2>
        {d.asiento.length === 0 ? <p className="text-sm text-gray-500">Esta devolución no tiene asiento registrado.</p> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-gray-500 border-b"><th className="py-2 px-3">Cuenta</th><th className="py-2 px-3">Nombre</th>
              <th className="py-2 px-3">Tercero / detalle</th><th className="py-2 px-3 text-right">Debe</th><th className="py-2 px-3 text-right">Haber</th></tr></thead>
            <tbody>
              {d.asiento.map((m, i) => (
                <tr key={i} className="border-b">
                  <td className="py-2 px-3 font-mono">{m.cuenta}</td><td className="py-2 px-3">{m.nombre}</td>
                  <td className="py-2 px-3 text-xs text-gray-600">{m.tercero && <div>{m.tercero}</div>}{m.detalle}</td>
                  <td className="py-2 px-3 text-right font-mono">{Number(m.debe) > 0 ? money(m.debe) : ''}</td>
                  <td className="py-2 px-3 text-right font-mono">{Number(m.haber) > 0 ? money(m.haber) : ''}</td>
                </tr>
              ))}
              <tr className="font-semibold"><td colSpan={3} className="py-2 px-3 text-right">Totales</td>
                <td className="py-2 px-3 text-right font-mono">{money(totalDebe)}</td><td className="py-2 px-3 text-right font-mono">{money(totalHaber)}</td></tr>
            </tbody>
          </table>
        )}
        <p className="text-xs text-gray-400 mt-2">Los asientos no se modifican ni se eliminan: cualquier corrección se registra con otro asiento.</p>
      </div>
    </div>
  );
}
