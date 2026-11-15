import catalogApi from './catalogApi';
const inventoryService = {
  getByWarehouse: (bodegaId) => catalogApi.get(`/api/inventory/warehouse/${bodegaId}`).then(r => r.data),
  // Paginado: { page, size, q }
  porBodegaPaginado: (bodegaId, params) => catalogApi.get(`/api/inventory/warehouse/${bodegaId}/paginado`, { params }).then(r => r.data),
  getByProduct: (productoId) => catalogApi.get(`/api/inventory/product/${productoId}`).then(r => r.data),
  getStock: (productoId) => catalogApi.get(`/api/inventory/product/${productoId}/stock`).then(r => r.data),
  adjust: (data) => catalogApi.post('/api/inventory/adjust', data).then(r => r.data),
  getMovements: (inventarioId) => catalogApi.get(`/api/inventory/movements/${inventarioId}`).then(r => r.data),
};
export default inventoryService;
