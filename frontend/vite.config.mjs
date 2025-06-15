import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools'
import { PrimeVueResolver } from '@primevue/auto-import-resolver'
import Components from 'unplugin-vue-components/vite'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  // Load environment variables based on mode
  const env = loadEnv(mode, process.cwd(), '')

  return {
    optimizeDeps: {
      noDiscovery: true,
    },
    plugins: [
      vue(),
      // Only enable dev tools in development mode
      ...(mode === 'development' ? [vueDevTools()] : []),
      Components({
        resolvers: [PrimeVueResolver()],
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    // Environment-specific configuration
    define: {
      __APP_VERSION__: JSON.stringify(env.VITE_APP_VERSION || '1.0.0'),
      __APP_NAME__: JSON.stringify(env.VITE_APP_NAME || 'LapXpert'),
    },
    // Build configuration
    build: {
      // Generate source maps only in development
      sourcemap: mode === 'development',
      // Minify in production
      minify: mode === 'production',
      // Optimize chunks for production
      rollupOptions: mode === 'production' ? {
        output: {
          manualChunks: {
            vendor: ['vue', 'vue-router', 'pinia'],
            primevue: ['primevue/config'],
            utils: ['axios', 'lodash-es']
          }
        }
      } : {}
    },
    // Development server configuration
    server: {
      port: parseInt(env.VITE_DEV_PORT) || 5173,
      host: env.VITE_DEV_HOST || 'localhost',
      // Proxy API requests in development
      proxy: mode === 'development' ? {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          secure: false
        }
      } : {}
    }
  }
})
