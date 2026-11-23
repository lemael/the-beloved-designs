import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authService } from '@/features/auth/services/auth.service'
import { storageService } from '@/services/storage.service'
import { toAuthUser } from '@/utils/auth-user'
import type { AuthResponse, AuthUser, LoginPayload, RegisterPayload } from '@/types/user.types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(storageService.getUser())
  const isAuthenticated = ref(!!storageService.getAccessToken())

  // Confort d'interface uniquement : /api/admin/** est protégé côté backend (403 sans rôle ADMIN).
  const isAdmin = computed(() => isAuthenticated.value && !!user.value?.roles.includes('ADMIN'))

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

  function applyAuthResponse(response: AuthResponse) {
    storageService.setTokens(response.accessToken, response.refreshToken)
    user.value = toAuthUser(response)
    storageService.setUser(user.value)
    isAuthenticated.value = true
  }

  return { user, isAuthenticated, isAdmin, login, register, logout }
})

