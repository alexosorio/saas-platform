import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { federation } from '@module-federation/vite'

export default defineConfig({
  base: '/catalog/',
  plugins: [
    react(),
    federation({
      name: 'catalog',
      dts: false,
      filename: 'remoteEntry.js',
      exposes: {
        './Routes': './src/remote/Routes.jsx',
        './PublicList': './src/components/lists/PublicListView.jsx',
        './Dashboard': './src/components/dashboard/Dashboard.jsx',
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
  server: { port: 5174, cors: true },
})
