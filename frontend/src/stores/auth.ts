import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api/bank'
import { TOKEN_KEY } from '../api/http'
import type { Staff } from '../types/models'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<Staff | null>(null)
  const permissions = ref<string[]>([])
  const initialized = ref(false)
  const loggedIn = computed(() => user.value !== null)
  const can = (permission: string) => permissions.value.includes(permission)

  function clear() {
    user.value = null
    permissions.value = []
    sessionStorage.removeItem(TOKEN_KEY)
  }
  async function initialize() {
    if (initialized.value) return
    if (sessionStorage.getItem(TOKEN_KEY)) {
      try {
        const me = await authApi.me()
        user.value = me.user
        permissions.value = me.permissions
      } catch {
        clear()
      }
    }
    initialized.value = true
  }
  async function login(username: string, password: string) {
    const result = await authApi.login(username, password)
    sessionStorage.setItem(TOKEN_KEY, result.token)
    try {
      const me = await authApi.me()
      user.value = me.user
      permissions.value = me.permissions
      initialized.value = true
    } catch (error) {
      clear()
      throw error
    }
  }
  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clear()
    }
  }
  return { user, permissions, loggedIn, can, initialize, login, logout, clear }
})
