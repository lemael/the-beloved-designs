import axios from 'axios'
import { storageService } from './storage.service'
import type { AuthResponse } from '@/types/user.types'

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
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = storageService.getRefreshToken()
      if (!refreshToken) {
        storageService.clearTokens()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const { data } = await api.post<AuthResponse>('/auth/refresh', { refreshToken })
        storageService.setTokens(data.accessToken, data.refreshToken)
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        // La tentative de rafraîchissement a échoué (token expiré) → on se déconnecte
        if (refreshError.response?.status === 401) {
          storageService.clearTokens()
          window.location.href = '/login'
          return Promise.reject(refreshError)
        }
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  }
)
