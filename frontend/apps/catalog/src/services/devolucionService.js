import catalogApi from './catalogApi';
const limpiar = (o = {}) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v != null));
const devolucionService = {
  listar: (filtros) => catalogApi.get('/api/devoluciones', { params: limpiar(filtros) }).then(r => r.data),
  paginado: (filtros) => catalogApi.get('/api/devoluciones/paginado', { params: limpiar(filtros) }).then(r => r.data),
  detalle: (id) => catalogApi.get(`/api/devoluciones/${id}`).then(r => r.data),
  buscarFacturas: (q) => catalogApi.get('/api/devoluciones/facturas', { params: { q } }).then(r => r.data),
};
export default devolucionService;
