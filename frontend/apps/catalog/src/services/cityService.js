import catalogApi from './catalogApi';
const cityService = {
  getAll: () => catalogApi.get('/api/cities').then(r => r.data),
  search: (q) => catalogApi.get('/api/cities', { params: { q } }).then(r => r.data),
};
export default cityService;
