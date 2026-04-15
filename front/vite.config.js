import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001,
    proxy: {
      '/api/o11y': {
        target: process.env.VITE_O11Y_API || 'http://localhost:18080',
        changeOrigin: true,
      },
      '/tumblebug': {
        target: process.env.VITE_TB_API || 'http://localhost:1323',
        changeOrigin: true,
      },
      '/spider': {
        target: process.env.VITE_SPIDER_API || 'http://localhost:1024',
        changeOrigin: true,
      },
    },
  },
});
