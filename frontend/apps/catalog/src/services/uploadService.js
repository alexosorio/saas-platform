import catalogApi from './catalogApi';

const uploadService = {
  uploadImage: async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    const res = await catalogApi.post('/api/uploads/image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;     // { url: '/uploads/products/...' }
  },
};

export default uploadService;
