import catalogApi from './catalogApi';
const limpiarP = (o = {}) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v !== null && v !== undefined && v !== false));
const clientService = {
  // Paginado para la tabla: { page, size, q }
  paginado: (params) => catalogApi.get('/api/clients/paginado', { params: limpiarP(params) }).then(r => r.data),
  getAll: () => catalogApi.get('/api/clients').then(r => r.data),
  search: (q) => catalogApi.get('/api/clients', { params: { q } }).then(r => r.data),
  getById: (id) => catalogApi.get(`/api/clients/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/clients', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/clients/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/clients/${id}`).then(r => r.data),
};
export default clientService;
