import type { NavigationGuardWithThis } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

export const requireAuth: NavigationGuardWithThis<undefined> = (to) => {
  const authStore = useAuthStore()
  if (!authStore.isAuthenticated) {
    return { path: '/account', query: { redirect: to.fullPath } }
  }
  return true
}
