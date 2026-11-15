import api from './subscriptionApi';
const paymentService = {
  getAll: () => api.get('/api/payments').then(r => r.data),
  getBySubscription: (id) => api.get(`/api/payments/subscription/${id}`).then(r => r.data),
  register: (data) => api.post('/api/payments', data).then(r => r.data),
};
export default paymentService;
