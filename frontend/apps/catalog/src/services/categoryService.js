import catalogApi from './catalogApi';
const categoryService = {
  getAll: () => catalogApi.get('/api/categories').then(r => r.data),
  getActive: () => catalogApi.get('/api/categories/active').then(r => r.data),
  getById: (id) => catalogApi.get(`/api/categories/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/categories', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/categories/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/categories/${id}`).then(r => r.data),
  activate: (id) => catalogApi.put(`/api/categories/${id}/activate`).then(r => r.data),
  deactivate: (id) => catalogApi.put(`/api/categories/${id}/deactivate`).then(r => r.data),
};
export default categoryService;
