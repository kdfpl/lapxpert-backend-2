import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import AuthService from '@/apis/auth'

import Aura from '@primeuix/themes/aura'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'

import '@/assets/styles.scss'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)

app.use(router)

app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '.app-dark',
    }
  },
  ripple: true,
})

app.use(ToastService)
app.use(ConfirmationService)

app.directive('tooltip', Tooltip)

app.config.errorHandler = (err, vm, info) => {
  console.error('Global error:', err)
}

app.mount('#app')

// Enhanced authentication state restoration
router.isReady().then(async () => {
  const currentRoute = router.currentRoute.value

  // Check authentication state on app startup
  if (currentRoute.meta.requiresAuth) {
    const isAuthenticated = AuthService.isAuthenticated()

    if (!isAuthenticated) {
      console.log('No valid authentication found - redirecting to login')
      router.push('/login')
    } else {
      // Optionally validate session with backend on startup
      try {
        const sessionValid = await AuthService.checkSession()
        if (!sessionValid) {
          console.log('Session validation failed on startup - redirecting to login')
          router.push('/login')
        } else {
          console.log('Authentication restored successfully')
        }
      } catch (error) {
        console.warn('Session validation error on startup:', error)
        // Don't redirect on network errors, let user continue
      }
    }
  }
})
