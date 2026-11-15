import catalogApi from './catalogApi';
const brandService = {
  getAll: () => catalogApi.get('/api/brands').then(r => r.data),
  getActive: () => catalogApi.get('/api/brands/active').then(r => r.data),
  getById: (id) => catalogApi.get(`/api/brands/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/brands', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/brands/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/brands/${id}`).then(r => r.data),
  activate: (id) => catalogApi.put(`/api/brands/${id}/activate`).then(r => r.data),
  deactivate: (id) => catalogApi.put(`/api/brands/${id}/deactivate`).then(r => r.data),
};
export default brandService;
