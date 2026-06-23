import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyPrefix = env.VITE_API_BASE_URL || '/api'
  return {
    base: env.VITE_BASE_URL || '/',
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        [apiProxyPrefix]: {
          target: env.VITE_DEV_API_PROXY || 'http://localhost:10002',
          changeOrigin: true
        }
      }
    }
  }
})
