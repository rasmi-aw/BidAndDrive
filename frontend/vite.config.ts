import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

// In dev, /api is proxied to the Spring Boot backend: the browser only talks to this origin (no CORS),
// and the Host header is kept, so the HAL links built by the backend point back to this dev server.
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: process.env.API_PROXY_TARGET ?? 'http://localhost:8080' },
    },
  },
  test: {
    environment: 'node',
    include: ['src/**/__tests__/*.spec.ts'],
  },
})
