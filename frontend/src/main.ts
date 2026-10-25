import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia).use(router)
window.addEventListener('session-expired', () => {
  useAuthStore(pinia).clear()
  void router.replace('/login')
})
app.mount('#app')
