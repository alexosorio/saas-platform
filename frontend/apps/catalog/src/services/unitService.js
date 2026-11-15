import catalogApi from './catalogApi';
const unitService = {
  getByProduct: (productoId) => catalogApi.get(`/api/units/product/${productoId}`).then(r => r.data),
  getById: (id) => catalogApi.get(`/api/units/${id}`).then(r => r.data),
  searchByImei: (imei) => catalogApi.get(`/api/units/imei/${imei}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/units', data).then(r => r.data),
  // Las unidades no se eliminan: se dan de baja (salida de inventario + nota crédito + auditoría)
  darDeBaja: (id, tipo, motivo) => catalogApi.post(`/api/units/${id}/baja`, { tipo, motivo }).then(r => r.data),
  // Asigna/cambia el código de barras de la unidad (vacío = quitar)
  setCodigoBarras: (id, codigoBarras) => catalogApi.put(`/api/units/${id}/codigo-barras`, { codigoBarras }).then(r => r.data),
  changeState: (id, estado) => catalogApi.put(`/api/units/${id}/state/${estado}`).then(r => r.data),
};
export default unitService;
