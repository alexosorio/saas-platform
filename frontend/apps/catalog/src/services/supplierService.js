import catalogApi from './catalogApi';
const supplierService = {
  search: (q) => catalogApi.get('/api/suppliers', { params: { q } }).then(r => r.data),
  getAll: () => catalogApi.get('/api/suppliers').then(r => r.data),
  getActive: () => catalogApi.get('/api/suppliers?soloActivos=true').then(r => r.data),
  getById: (id) => catalogApi.get(`/api/suppliers/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/suppliers', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/suppliers/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/suppliers/${id}`).then(r => r.data),
};
export default supplierService;
