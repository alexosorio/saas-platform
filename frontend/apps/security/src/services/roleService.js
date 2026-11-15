import api from './api';

const roleService = {
  async getAllRoles() {
    const response = await api.get('/api/roles');
    return response.data;
  },

  async getRoleById(id) {
    const response = await api.get(`/api/roles/${id}`);
    return response.data;
  },

  async createRole(roleData) {
    const response = await api.post('/api/roles', roleData);
    return response.data;
  },

  async updateRole(id, roleData) {
    const response = await api.put(`/api/roles/${id}`, roleData);
    return response.data;
  },

  async deleteRole(id) {
    const response = await api.delete(`/api/roles/${id}`);
    return response.data;
  },

  async personalizar(id, nombre) {
    const response = await api.post(`/api/roles/${id}/personalizar`, nombre ? { nombre } : {});
    return response.data;
  },

  async getModulos() {
    const response = await api.get('/api/roles/modulos');
    return response.data;
  },

  // permisos: [{ moduloId, crear, leer, actualizar, eliminar }]
  async updatePermisos(id, permisos) {
    const response = await api.put(`/api/roles/${id}/permisos`, permisos);
    return response.data;
  }
};

export default roleService;
