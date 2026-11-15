import catalogApi from './catalogApi';

// Quita filtros vacíos para no enviar parámetros en blanco
const limpiar = (f = {}) => Object.fromEntries(Object.entries(f).filter(([, v]) => v !== '' && v !== null && v !== undefined && v !== false));

const cuentaService = {
  // filtros: { estado, proveedorId, desde, hasta, campoFecha, soloVencidas }
  porPagar: (filtros) => catalogApi.get('/api/cuentas/por-pagar', { params: limpiar(filtros) }).then(r => r.data),
  // filtros: { estado, clienteId, desde, hasta, campoFecha, soloVencidas }
  porCobrar: (filtros) => catalogApi.get('/api/cuentas/por-cobrar', { params: limpiar(filtros) }).then(r => r.data),
  // Paginados: mismos filtros + { page, size }; resumen.saldoPendiente = total del filtro
  porPagarPaginado: (filtros) => catalogApi.get('/api/cuentas/por-pagar/paginado', { params: limpiar(filtros) }).then(r => r.data),
  porCobrarPaginado: (filtros) => catalogApi.get('/api/cuentas/por-cobrar/paginado', { params: limpiar(filtros) }).then(r => r.data),
  pagos: (id) => catalogApi.get(`/api/cuentas/por-pagar/${id}/pagos`).then(r => r.data),
  registrarPago: (id, data) => catalogApi.post(`/api/cuentas/por-pagar/${id}/pagos`, data).then(r => r.data),
};
export default cuentaService;
