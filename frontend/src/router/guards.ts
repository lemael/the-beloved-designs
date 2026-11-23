import type { NavigationGuardWithThis, RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

export const requireAuth: NavigationGuardWithThis<undefined> = (to) => {
  const authStore = useAuthStore()
  if (!authStore.isAuthenticated) {
    return { path: '/account', query: { redirect: to.fullPath } }
  }
  return true
}

/**
 * Routes du back-office (meta.requiresAdmin). Non connecté ou sans rôle ADMIN → page de connexion admin.
 * Confort d'interface uniquement : la vraie protection est côté backend (/api/admin/** → 403).
 */
export function requireAdmin(to: Pick<RouteLocationNormalized, 'meta' | 'fullPath'>) {
  // meta fusionnée parent → enfant : la page de connexion redéfinit requiresAdmin à false
  if (!to.meta.requiresAdmin) {
    return true
  }
  const authStore = useAuthStore()
  if (!authStore.isAdmin) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  return true
}
