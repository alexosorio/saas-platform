import api from './subscriptionApi';
const planService = {
  getAll: () => api.get('/api/plans').then(r => r.data),
  getActive: () => api.get('/api/plans/active').then(r => r.data),
  getById: (id) => api.get(`/api/plans/${id}`).then(r => r.data),
};
export default planService;
