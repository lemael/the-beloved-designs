import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router'
import { requireAdmin } from '@/router/guards'
import { storageService } from '@/services/storage.service'
import { useAuthStore } from '@/stores/auth.store'
import type { AuthUser } from '@/types/user.types'

function loginAs(roles: AuthUser['roles']) {
  storageService.setTokens('access', 'refresh')
  storageService.setUser({ phone: '+237600000001', firstName: 'A', lastName: 'B', roles })
  setActivePinia(createPinia())
}

describe('routes /admin', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it("/admin affiche la page de connexion (pas le layout)", () => {
    const resolved = router.resolve('/admin')
    expect(resolved.name).toBe('admin-login')
    expect(resolved.meta.requiresAdmin).toBe(false)
  })

  it.each([
    ['/admin/dashboard', 'admin-dashboard'],
    ['/admin/products', 'admin-products'],
    ['/admin/products/new', 'admin-product-new'],
    ['/admin/stock', 'admin-stock'],
    ['/admin/restocks', 'admin-restocks'],
    ['/admin/reports/yearly', 'admin-yearly-report'],
  ])('%s est une route protégée (%s)', (path, name) => {
    const resolved = router.resolve(path)
    expect(resolved.name).toBe(name)
    expect(resolved.meta.requiresAdmin).toBe(true)
  })

  it('une route admin inconnue tombe sur la page 404', () => {
    expect(router.resolve('/admin/inconnu').name).toBe('not-found')
  })

  it('anonyme → redirigé vers la connexion admin avec redirect', () => {
    const result = requireAdmin(router.resolve('/admin/stock'))
    expect(result).toEqual({ name: 'admin-login', query: { redirect: '/admin/stock' } })
  })

  it('client connecté sans rôle ADMIN → redirigé', () => {
    loginAs(['CLIENT'])
    expect(requireAdmin(router.resolve('/admin/dashboard'))).not.toBe(true)
  })

  it('admin connecté → accès autorisé', () => {
    loginAs(['ADMIN', 'CLIENT'])
    expect(requireAdmin(router.resolve('/admin/dashboard'))).toBe(true)
  })

  it('les routes de la boutique ne sont pas concernées', () => {
    expect(requireAdmin(router.resolve('/products'))).toBe(true)
    expect(requireAdmin(router.resolve('/admin'))).toBe(true)
  })
})

describe('auth store — isAdmin', () => {
  beforeEach(() => localStorage.clear())

  it('restaure les rôles après rechargement de la page', () => {
    loginAs(['ADMIN'])
    expect(useAuthStore().isAdmin).toBe(true)
  })

  it('profil sauvegardé sans rôles (ancienne version) → pas admin', () => {
    storageService.setTokens('access', 'refresh')
    localStorage.setItem('tbd_user', JSON.stringify({ phone: '+237600000001', firstName: 'A', lastName: 'B' }))
    setActivePinia(createPinia())
    expect(useAuthStore().isAdmin).toBe(false)
  })

  it('rôle ADMIN mais sans token → pas admin', () => {
    storageService.setUser({ phone: '+237600000001', firstName: 'A', lastName: 'B', roles: ['ADMIN'] })
    setActivePinia(createPinia())
    expect(useAuthStore().isAdmin).toBe(false)
  })

  it('logout efface le profil et les rôles', () => {
    loginAs(['ADMIN'])
    const store = useAuthStore()
    store.logout()
    expect(store.isAdmin).toBe(false)
    expect(storageService.getUser()).toBeNull()
  })
})
