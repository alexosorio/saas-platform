import userApi from './userApi';

const accountService = {
  getMe: () => userApi.get('/api/users/me').then(r => r.data),
  changePassword: (data) => userApi.post('/api/users/change-password', data).then(r => r.data),
};
export default accountService;
