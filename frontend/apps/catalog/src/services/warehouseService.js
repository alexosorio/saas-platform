import catalogApi from './catalogApi';
const warehouseService = {
  getAll: () => catalogApi.get('/api/warehouses').then(r => r.data),
  getBySede: (sedeId) => catalogApi.get(`/api/warehouses/by-sede/${sedeId}`).then(r => r.data),
  getById: (id) => catalogApi.get(`/api/warehouses/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/warehouses', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/warehouses/${id}`, data).then(r => r.data),
  activate: (id) => catalogApi.put(`/api/warehouses/${id}/activate`).then(r => r.data),
  deactivate: (id) => catalogApi.put(`/api/warehouses/${id}/deactivate`).then(r => r.data),
};
export default warehouseService;
