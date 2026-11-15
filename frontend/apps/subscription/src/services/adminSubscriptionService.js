import api from './subscriptionApi';

// Administración de suscripciones de la PLATAFORMA (todas las organizaciones)
const BASE = '/api/admin/subscriptions';
const limpiar = (o = {}) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v != null));

const adminSubscriptionService = {
  suscripciones: (filtros) => api.get(BASE, { params: limpiar(filtros) }).then(r => r.data),
  activar: (id, motivo) => api.put(`${BASE}/${id}/activate`, { motivo }),
  suspender: (id, motivo) => api.put(`${BASE}/${id}/suspend`, { motivo }),
  pagos: (estado) => api.get(`${BASE}/payments`, { params: limpiar({ estado }) }).then(r => r.data),
  aprobarPago: (id) => api.put(`${BASE}/payments/${id}/approve`).then(r => r.data),
  rechazarPago: (id, motivo) => api.put(`${BASE}/payments/${id}/reject`, { motivo }).then(r => r.data),
  facturacion: (desde, hasta) => api.get(`${BASE}/billing`, { params: limpiar({ desde, hasta }) }).then(r => r.data),
  ejecutarVencimientos: () => api.post(`${BASE}/run-expirations`).then(r => r.data),
};
export default adminSubscriptionService;
