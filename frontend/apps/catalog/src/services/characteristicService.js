import catalogApi from './catalogApi';
const characteristicService = {
  getAll: () => catalogApi.get('/api/characteristics').then(r => r.data),
  getByType: (tipo) => catalogApi.get(`/api/characteristics/by-type/${tipo}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/characteristics', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/characteristics/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/characteristics/${id}`).then(r => r.data),
};
export default characteristicService;
