#!/bin/bash

cd "$(dirname "$0")/src"

echo "Creando todos los archivos del frontend React..."

# Ya tenemos api.js y authService.js, crear los demás servicios

# userService.js
cat > services/userService.js << 'EOF'
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

  async changePassword(currentPassword, newPassword) {
    const response = await api.post('/api/users/change-password', {
      currentPassword,
      newPassword
    });
    return response.data;
  }
};

export default userService;
EOF

# roleService.js
cat > services/roleService.js << 'EOF'
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
  }
};

export default roleService;
EOF

echo "✅ Servicios creados (4 archivos)"
echo "Archivos creados:"
ls -1 services/
