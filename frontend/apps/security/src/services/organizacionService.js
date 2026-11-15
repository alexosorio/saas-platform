import api from './api';

/** Marca de la organización: nombre y logo (data URL). Avisa con el evento 'marca-actualizada'. */
const avisar = (marca) => { window.dispatchEvent(new CustomEvent('marca-actualizada', { detail: marca })); return marca; };

const organizacionService = {
  getMarca: () => api.get('/api/organizacion/marca').then(r => r.data),
  setLogo: (logo) => api.put('/api/organizacion/logo', { logo }).then(r => avisar(r.data)),
  quitarLogo: () => api.delete('/api/organizacion/logo').then(r => avisar(r.data)),
};
export default organizacionService;
