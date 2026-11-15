import catalogApi from './catalogApi';
const limpiarP = (o = {}) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v !== null && v !== undefined && v !== false));
const orderService = {
  // Paginado para la tabla: { page, size, estado, q } -> { contenido, totalElementos, totalPaginas, ... }
  paginado: (params) => catalogApi.get('/api/orders/paginado', { params: limpiarP(params) }).then(r => r.data),
  getAll: () => catalogApi.get('/api/orders').then(r => r.data),
  getByState: (estado) => catalogApi.get(`/api/orders/by-state/${estado}`).then(r => r.data),
  getById: (id) => catalogApi.get(`/api/orders/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/orders', data).then(r => r.data),
  confirm: (id) => catalogApi.put(`/api/orders/${id}/confirm`).then(r => r.data),
  reserve: (id) => catalogApi.put(`/api/orders/${id}/reserve`).then(r => r.data),
  reverse: (id) => catalogApi.put(`/api/orders/${id}/reverse`).then(r => r.data),
  updateStatus: (id, estado) => catalogApi.put(`/api/orders/${id}/status`, { estado }).then(r => r.data),
  cancel: (id, motivo) => catalogApi.put(`/api/orders/${id}/cancel`, { motivo }).then(r => r.data),
  registerPayment: (id, data) => catalogApi.post(`/api/orders/${id}/payments`, data).then(r => r.data),
  getPayments: (id) => catalogApi.get(`/api/orders/${id}/payments`).then(r => r.data),
  // Devoluciones de venta y saldo a favor del cliente
  devolver: (id, data) => catalogApi.post(`/api/orders/${id}/devoluciones`, data).then(r => r.data),
  getDevoluciones: (id) => catalogApi.get(`/api/orders/${id}/devoluciones`).then(r => r.data),
  saldoCliente: (id) => catalogApi.get(`/api/orders/${id}/saldo-cliente`).then(r => r.data),
  // color: elegir unidad de ese color (búsqueda por SKU/código de barras); excluir: unidades ya en la orden
  // sedeId: sede de la orden; se prefieren unidades que estén en ella
  scan: (codigo, listaPreciosId, { color, excluir = [], sedeId } = {}) => catalogApi.get('/api/orders/scan', {
    params: {
      codigo, listaPreciosId,
      ...(sedeId ? { sedeId } : {}),
      ...(color ? { color } : {}),
      ...(excluir.length ? { excluir: excluir.join(',') } : {}),
    },
  }).then(r => r.data),
  // Sede con la que arranca la orden del usuario y si puede elegir otra (permiso ventas_otra_bodega)
  contextoVenta: () => catalogApi.get('/api/usuarios-bodegas/me/contexto-venta').then(r => r.data),
};
export default orderService;
