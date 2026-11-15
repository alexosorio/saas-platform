import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { federation } from '@module-federation/vite'

export default defineConfig({
  base: '/subscription/',
  plugins: [
    react(),
    federation({
      name: 'subscription',
      dts: false,
      filename: 'remoteEntry.js',
      exposes: { './Routes': './src/remote/Routes.jsx' },
      shared: {
        react: { singleton: true, requiredVersion: false },
        'react-dom': { singleton: true, requiredVersion: false },
        'react-router-dom': { singleton: true, requiredVersion: false },
        '@saas/ui-shell': { singleton: true, requiredVersion: false },
      },
    }),
  ],
  build: { target: 'esnext' },
  server: { port: 5175, cors: true },
})
