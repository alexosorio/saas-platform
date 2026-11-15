import axios from 'axios';

// Cliente para el user-service (perfil, cambio de contraseña).
const USER_URL = import.meta.env.VITE_USER_API_URL || 'http://localhost:8082';

const userApi = axios.create({ baseURL: USER_URL });

userApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default userApi;
