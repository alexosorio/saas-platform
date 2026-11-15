import catalogApi from './catalogApi';
const dashboardService = {
  resumen: ({ dias = 30, umbral = null } = {}) =>
    catalogApi.get('/api/dashboard', { params: { dias, ...(umbral != null ? { umbral } : {}) } }).then(r => r.data),
};
export default dashboardService;
