import type { AuthUser } from '@/types/user.types'

const ACCESS_TOKEN_KEY = 'tbd_access_token'
const REFRESH_TOKEN_KEY = 'tbd_refresh_token'
const USER_KEY = 'tbd_user'

export const storageService = {
  getAccessToken: (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string): void => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
  clearTokens: (): void => {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },

  // Profil (dont les rôles) conservé pour survivre au rechargement de la page.
  // Sert uniquement à l'affichage : l'autorisation réelle est vérifiée par le backend.
  getUser: (): AuthUser | null => {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try {
      const user = JSON.parse(raw) as AuthUser
      return { ...user, roles: Array.isArray(user.roles) ? user.roles : [] }
    } catch {
      return null
    }
  },
  setUser: (user: AuthUser): void => {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },
}
