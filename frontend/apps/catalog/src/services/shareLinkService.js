import catalogApi from './catalogApi';
import publicCatalogApi from './publicCatalogApi';

const shareLinkService = {
  // ===== Gestion (autenticado) =====
  listByList: (listaId) => catalogApi.get(`/api/lists/${listaId}/links`).then(r => r.data),
  create: (listaId, data) => catalogApi.post(`/api/lists/${listaId}/links`, data).then(r => r.data),
  sendEmail: (listaId, enlaceId, data) =>
    catalogApi.post(`/api/lists/${listaId}/links/${enlaceId}/send-email`, data).then(r => r.data),
  sendWhatsApp: (listaId, enlaceId, data) =>
    catalogApi.post(`/api/lists/${listaId}/links/${enlaceId}/send-whatsapp`, data).then(r => r.data),
  sendBulk: (listaId, enlaceId, data) =>
    catalogApi.post(`/api/lists/${listaId}/links/${enlaceId}/send-bulk`, data).then(r => r.data),
  deactivate: (listaId, enlaceId) => catalogApi.put(`/api/lists/${listaId}/links/${enlaceId}/deactivate`).then(r => r.data),
  delete: (listaId, enlaceId) => catalogApi.delete(`/api/lists/${listaId}/links/${enlaceId}`).then(r => r.data),

  // ===== Vista publica (sin autenticacion) =====
  viewPublic: (token) => publicCatalogApi.get(`/api/public/lists/${token}`).then(r => r.data),
  viewPublicWithPassword: (token, password) =>
    publicCatalogApi.post(`/api/public/lists/${token}/access`, { password }).then(r => r.data),
};

export default shareLinkService;
