import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { setTokenProvider } from './api/http'
import './assets/main.css'
import { initKeycloak, keycloak } from './auth/keycloak'
import { router } from './router'
import { useAuthStore } from './stores/auth'

async function bootstrap(): Promise<void> {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)

  const auth = useAuthStore(pinia)
  setTokenProvider(() => auth.accessToken())
  keycloak.onAuthRefreshSuccess = () => auth.setSession(keycloak.tokenParsed)
  keycloak.onAuthLogout = () => auth.clearSession()
  try {
    await initKeycloak()
    auth.setSession(keycloak.tokenParsed)
  } catch (error) {
    // the public list still works without Keycloak
    console.warn('Keycloak is not reachable, continuing anonymously', error)
    auth.markUnavailable()
  }

  app.use(router)
  app.mount('#app')
}

void bootstrap()
