import axios from 'axios';

// Cliente axios SIN token, para los endpoints publicos de /api/public/**
// Lo usa la vista publica de listas (cuando un visitante abre un enlace compartido).
const CATALOG_URL = import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8083';

const publicCatalogApi = axios.create({
  baseURL: CATALOG_URL,
  headers: { 'Content-Type': 'application/json' },
});

export default publicCatalogApi;
