import api from './subscriptionApi';
const paymentMethodService = {
  getAll: () => api.get('/api/payment-methods').then(r => r.data),
  create: (data) => api.post('/api/payment-methods', data).then(r => r.data),
  markDefault: (id) => api.put(`/api/payment-methods/${id}/default`).then(r => r.data),
  delete: (id) => api.delete(`/api/payment-methods/${id}`).then(r => r.data),
};
export default paymentMethodService;
