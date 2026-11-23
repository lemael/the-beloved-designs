import axios from 'axios'
import { storageService } from './storage.service'
import type { AuthResponse } from '@/types/user.types'
import { toAuthUser } from '@/utils/auth-user'

// Page de connexion à afficher quand la session est perdue (la route /login n'existe pas).
function loginPath(): string {
  return window.location.pathname.startsWith('/admin') ? '/admin' : '/account'
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
  const token = storageService.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    // Les appels /auth/* (login, register, refresh) gèrent eux-mêmes leur 401 : un mauvais mot de passe
    // n'est pas une session expirée (sinon la page est rechargée et le message d'erreur disparaît).
    const isAuthCall = typeof originalRequest?.url === 'string' && originalRequest.url.startsWith('/auth/')
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthCall) {
      originalRequest._retry = true
      const refreshToken = storageService.getRefreshToken()
      if (!refreshToken) {
        storageService.clearTokens()
        window.location.href = loginPath()
        return Promise.reject(error)
      }

      try {
        const { data } = await api.post<AuthResponse>('/auth/refresh', { refreshToken })
        storageService.setTokens(data.accessToken, data.refreshToken)
        storageService.setUser(toAuthUser(data))
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        // La tentative de rafraîchissement a échoué (token expiré) → on se déconnecte
        if (axios.isAxiosError(refreshError) && refreshError.response?.status === 401) {
          storageService.clearTokens()
          window.location.href = loginPath()
          return Promise.reject(refreshError)
        }
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  }
)
