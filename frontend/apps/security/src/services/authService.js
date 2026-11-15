import api from './api';

const authService = {
  async login(email, password) {
    const response = await api.post('/api/auth/login', { email, password });
    if (response.data.accessToken) {
      localStorage.setItem('accessToken', response.data.accessToken);
      localStorage.setItem('refreshToken', response.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
  },

  async register(userData) {
    const response = await api.post('/api/auth/register', userData);
    return response.data;
  },

  async verifyEmail(email, code) {
    const response = await api.post('/api/users/verify-email', { email, code });
    return response.data;
  },

  async resendVerificationCode(email) {
    const response = await api.post('/api/users/resend-verification', null, {
      params: { email }
    });
    return response.data;
  },

  // Verificación con el enlace del correo
  async verificarEmail(token) {
    const response = await api.post('/api/auth/verificar-email', { token });
    return response.data;
  },

  // Enlace nuevo: por correo o a partir de un enlace vencido (respuesta genérica)
  async reenviarVerificacion({ email, token } = {}) {
    const response = await api.post('/api/auth/reenviar-verificacion', { email, token });
    return response.data;
  },

  async forgotPassword(email) {
    const response = await api.post('/api/users/forgot-password', { email });
    return response.data;
  },

  async requestPasswordResetLink(email) {
    const response = await api.post('/api/users/request-password-reset', null, {
      params: { email }
    });
    return response.data;
  },

  async resetPasswordWithCode(email, code, newPassword) {
    const response = await api.post('/api/users/reset-password-code', {
      email,
      code,
      newPassword
    });
    return response.data;
  },

  async resetPasswordWithToken(token, newPassword) {
    const response = await api.post('/api/users/reset-password-token', {
      token,
      newPassword
    });
    return response.data;
  },

  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  getToken() {
    return localStorage.getItem('accessToken');
  },

  isAuthenticated() {
    return !!this.getToken();
  }
};

export default authService;
