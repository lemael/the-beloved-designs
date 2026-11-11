import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authService } from '@/features/auth/services/auth.service'
import { storageService } from '@/services/storage.service'
import type { AuthUser, LoginPayload, RegisterPayload } from '@/types/user.types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const isAuthenticated = ref(!!storageService.getAccessToken())

  async function login(payload: LoginPayload) {
    const response = await authService.login(payload)
    applyAuthResponse(response)
  }

  async function register(payload: RegisterPayload) {
    const response = await authService.register(payload)
    applyAuthResponse(response)
  }

  function logout() {
    storageService.clearTokens()
    user.value = null
    isAuthenticated.value = false
  }

  function applyAuthResponse(response: { accessToken: string; refreshToken: string; email: string; firstName: string; lastName: string }) {
    storageService.setTokens(response.accessToken, response.refreshToken)
    user.value = { email: response.email, firstName: response.firstName, lastName: response.lastName }
    isAuthenticated.value = true
  }

  return { user, isAuthenticated, login, register, logout }
})
