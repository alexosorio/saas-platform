import catalogApi from './catalogApi';
const listService = {
  getAll: () => catalogApi.get('/api/lists').then(r => r.data),
  getActive: () => catalogApi.get('/api/lists/active').then(r => r.data),
  getById: (id) => catalogApi.get(`/api/lists/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/lists', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/lists/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/lists/${id}`).then(r => r.data),
  activate: (id) => catalogApi.put(`/api/lists/${id}/activate`).then(r => r.data),
  deactivate: (id) => catalogApi.put(`/api/lists/${id}/deactivate`).then(r => r.data),

  // Modelo nuevo: precios calculados de todos los productos activos
  computedPrices: (listaId) => catalogApi.get(`/api/lists/${listaId}/computed-prices`).then(r => r.data),

  // Overrides manuales (excepciones puntuales sobre la formula)
  getOverrides: (listaId) => catalogApi.get(`/api/lists/${listaId}/prices`).then(r => r.data),
  addOverride: (listaId, data) => catalogApi.post(`/api/lists/${listaId}/prices`, data).then(r => r.data),
  clearOverrides: (listaId) => catalogApi.delete(`/api/lists/${listaId}/prices`).then(r => r.data),
  deleteOverride: (precioId) => catalogApi.delete(`/api/lists/prices/${precioId}`).then(r => r.data),
};
export default listService;
