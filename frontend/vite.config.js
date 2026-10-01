import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// During `npm run dev` each API prefix is proxied to the service that owns it,
// mirroring the nginx routing used in Docker and the Ingress used in Kubernetes.
export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 800,
  },
  server: {
    port: 5173,
    proxy: {
      '/api/users': 'http://localhost:8081',
      '/api/restaurants': 'http://localhost:8082',
      '/api/orders': 'http://localhost:8083',
      '/api/billing': 'http://localhost:8084',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
});
