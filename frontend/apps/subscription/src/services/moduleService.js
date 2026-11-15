import api from './subscriptionApi';
const moduleService = {
  getActive: () => api.get('/api/modules').then(r => r.data),
};
export default moduleService;
