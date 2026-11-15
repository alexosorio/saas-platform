import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { federation } from '@module-federation/vite'

export default defineConfig({
  base: '/',
  plugins: [
    react(),
    federation({
      name: 'host',
      dts: false,
      // URL relativa: en dev el proxy (abajo) reenvía todo /catalog y /subscription
      // a sus dev servers; en build, nginx los sirve bajo el mismo origen.
      remotes: {
        catalog: { type: 'module', name: 'catalog', entry: '/catalog/remoteEntry.js' },
        subscription: { type: 'module', name: 'subscription', entry: '/subscription/remoteEntry.js' },
      },
      shared: {
        react: { singleton: true, requiredVersion: false },
        'react-dom': { singleton: true, requiredVersion: false },
        'react-router-dom': { singleton: true, requiredVersion: false },
        '@saas/ui-shell': { singleton: true, requiredVersion: false },
      },
    }),
  ],
  build: { target: 'esnext' },
  server: {
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:8082', changeOrigin: true },
      // Reenvía TODO el prefijo del remote a su dev server (incluye remoteEntry,
      // /assets, /node_modules/.vite/deps, /src, /@id, HMR ws...). Single-origin en dev.
      '/catalog': { target: 'http://localhost:5174', changeOrigin: true, ws: true },
      '/subscription': { target: 'http://localhost:5175', changeOrigin: true, ws: true },
    },
  },
})
