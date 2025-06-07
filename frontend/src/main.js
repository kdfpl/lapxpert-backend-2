import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

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

router.isReady().then(() => {
  const isAuthenticated = localStorage.getItem('token')
  const currentRoute = router.currentRoute.value
  
  if (!isAuthenticated && currentRoute.meta.requiresAuth) {
    router.push('/login')
  }
})