import catalogApi from './catalogApi';
const sedeService = {
  getAll: () => catalogApi.get('/api/branches').then(r => r.data),
  getActive: () => catalogApi.get('/api/branches?soloActivas=true').then(r => r.data),
  getById: (id) => catalogApi.get(`/api/branches/${id}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/branches', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/branches/${id}`, data).then(r => r.data),
  activate: (id) => catalogApi.put(`/api/branches/${id}/activate`).then(r => r.data),
  deactivate: (id) => catalogApi.put(`/api/branches/${id}/deactivate`).then(r => r.data),
};
export default sedeService;
