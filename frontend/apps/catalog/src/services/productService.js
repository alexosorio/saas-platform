import catalogApi from './catalogApi';
const limpiarP = (o = {}) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v !== null && v !== undefined && v !== false));
const productService = {
  // Paginado para la tabla: { page, size, q, activo }
  paginado: (params) => catalogApi.get('/api/products/paginado', { params: limpiarP(params) }).then(r => r.data),
  getAll: () => catalogApi.get('/api/products').then(r => r.data),
  getActive: () => catalogApi.get('/api/products/active').then(r => r.data),
  getById: (id) => catalogApi.get(`/api/products/${id}`).then(r => r.data),
  search: (q) => catalogApi.get('/api/products/search', { params: { q } }).then(r => r.data),
  byBarcode: (codigo) => catalogApi.get(`/api/products/by-barcode/${encodeURIComponent(codigo)}`).then(r => r.data),
  create: (data) => catalogApi.post('/api/products', data).then(r => r.data),
  update: (id, data) => catalogApi.put(`/api/products/${id}`, data).then(r => r.data),
  delete: (id) => catalogApi.delete(`/api/products/${id}`).then(r => r.data),
  activate: (id) => catalogApi.put(`/api/products/${id}/activate`).then(r => r.data),
  deactivate: (id) => catalogApi.put(`/api/products/${id}/deactivate`).then(r => r.data),
  priceHistory: (id) => catalogApi.get(`/api/products/${id}/price-history`).then(r => r.data),
  bulkUpload: async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    const res = await catalogApi.post('/api/products/bulk', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },
};
export default productService;
