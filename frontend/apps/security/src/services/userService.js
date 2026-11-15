import api from './api';

const userService = {
  async getAllUsers() {
    const response = await api.get('/api/users');
    return response.data;
  },

  async getUserById(id) {
    const response = await api.get(`/api/users/${id}`);
    return response.data;
  },

  async getCurrentUser() {
    const response = await api.get('/api/users/me');
    return response.data;
  },

  async createUser(userData) {
    const response = await api.post('/api/users', userData);
    return response.data;
  },

  async updateUser(id, userData) {
    const response = await api.put(`/api/users/${id}`, userData);
    return response.data;
  },

  async deleteUser(id) {
    const response = await api.delete(`/api/users/${id}`);
    return response.data;
  },

  async activateUser(id) {
    const response = await api.put(`/api/users/${id}/activate`);
    return response.data;
  },

  async deactivateUser(id) {
    const response = await api.put(`/api/users/${id}/deactivate`);
    return response.data;
  },

  // Verificación de correo
  async reenviarVerificacionPropia() {
    const response = await api.post('/api/users/me/reenviar-verificacion');
    return response.data;
  },

  async reenviarVerificacion(id) {
    const response = await api.post(`/api/users/${id}/reenviar-verificacion`);
    return response.data;
  },

  async verificarManual(id) {
    const response = await api.put(`/api/users/${id}/verificar-email`);
    return response.data;
  },

  async changePassword(currentPassword, newPassword) {
    const response = await api.post('/api/users/change-password', {
      currentPassword,
      newPassword
    });
    return response.data;
  }
};

export default userService;
