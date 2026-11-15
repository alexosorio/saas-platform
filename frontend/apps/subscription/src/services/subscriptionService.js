import api from './subscriptionApi';
const subscriptionService = {
  getCurrent: () => api.get('/api/subscription/current').then(r => r.data),
  getAll: () => api.get('/api/subscription/all').then(r => r.data),
  subscribe: (data) => api.post('/api/subscription', data).then(r => r.data),
  cancel: (motivo) => api.put('/api/subscription/cancel', { motivo }).then(r => r.data),
  reactivate: () => api.put('/api/subscription/reactivate').then(r => r.data),
  setAutoRenewal: (autoRenovacion) => api.put('/api/subscription/auto-renewal', { autoRenovacion }).then(r => r.data),
  getHistory: (id) => api.get(`/api/subscription/${id}/history`).then(r => r.data),
};
export default subscriptionService;
