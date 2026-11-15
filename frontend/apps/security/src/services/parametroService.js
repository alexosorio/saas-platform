import api from './api';
const parametroService = {
  listar: () => api.get('/api/parametros').then(r => r.data),
  actualizar: (clave, valor, general = false) => api.put(`/api/parametros/${encodeURIComponent(clave)}`, { valor, general }),
  restablecer: (clave) => api.delete(`/api/parametros/${encodeURIComponent(clave)}/organizacion`),
  sesion: () => api.get('/api/parametros/sesion').then(r => r.data),
};
export default parametroService;
