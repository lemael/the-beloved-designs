import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError, AxiosHeaders } from 'axios'
import { MAX_QUANTITY_PER_ADD, useAddToCart } from '@/features/cart/composables/useAddToCart'
import { useAuthStore } from '@/stores/auth.store'
import { useCartStore } from '@/stores/cart.store'
import type { Product } from '@/types/product.types'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ fullPath: '/products/robe-satin' }),
}))

function product(stockQuantity: number): Product {
  return { id: 7, name: 'Robe satin', slug: 'robe-satin', price: 10000, stockQuantity } as Product
}

/** Erreur 409 au format ApiError du backend. */
function stockError(): AxiosError {
  const headers = new AxiosHeaders()
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', { headers }, null, {
    status: 409,
    statusText: 'Conflict',
    headers,
    config: { headers },
    data: { status: 409, code: 'INSUFFICIENT_STOCK', message: 'Stock insuffisant : plus que 2 disponible(s)' },
  })
}

function logIn() {
  const auth = useAuthStore()
  auth.isAuthenticated = true
}

describe('useAddToCart', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockReset()
  })

  it('visiteur non connecté : renvoie vers la connexion avec retour sur la fiche, sans appel API', async () => {
    const addItem = vi.spyOn(useCartStore(), 'addItem')
    const { add } = useAddToCart(ref(product(5)))

    await add()

    expect(push).toHaveBeenCalledWith({ path: '/account', query: { redirect: '/products/robe-satin' } })
    expect(addItem).not.toHaveBeenCalled()
  })

  it('client connecté : ajoute la quantité choisie puis passe en « ajouté »', async () => {
    logIn()
    const addItem = vi.spyOn(useCartStore(), 'addItem').mockResolvedValue()
    const { add, quantity, status } = useAddToCart(ref(product(5)))
    quantity.value = 3

    await add()

    expect(addItem).toHaveBeenCalledWith(7, 3)
    expect(status.value).toBe('added')
  })

  it('double clic pendant l\'envoi : un seul ajout', async () => {
    logIn()
    let finish!: () => void
    const addItem = vi
      .spyOn(useCartStore(), 'addItem')
      .mockReturnValue(new Promise<void>((resolve) => (finish = resolve)))
    const { add, status } = useAddToCart(ref(product(5)))

    const first = add()
    expect(status.value).toBe('adding')
    await add()
    finish()
    await first

    expect(addItem).toHaveBeenCalledTimes(1)
    expect(status.value).toBe('added')
  })

  it('stock insuffisant : affiche le message du serveur', async () => {
    logIn()
    vi.spyOn(useCartStore(), 'addItem').mockRejectedValue(stockError())
    const { add, status, error } = useAddToCart(ref(product(5)))

    await add()

    expect(status.value).toBe('error')
    expect(error.value).toBe('Stock insuffisant : plus que 2 disponible(s)')
  })

  it('erreur sans réponse du serveur : message de secours', async () => {
    logIn()
    vi.spyOn(useCartStore(), 'addItem').mockRejectedValue(new Error('Network Error'))
    const { add, error } = useAddToCart(ref(product(5)))

    await add()

    expect(error.value).toBe("Impossible d'ajouter ce produit au panier. Réessayez dans un instant.")
  })

  it('rupture de stock : bouton inactif, aucun appel', async () => {
    logIn()
    const addItem = vi.spyOn(useCartStore(), 'addItem')
    const { add, inStock } = useAddToCart(ref(product(0)))

    await add()

    expect(inStock.value).toBe(false)
    expect(addItem).not.toHaveBeenCalled()
  })

  it('quantité proposée plafonnée par le stock et par le maximum par ajout', () => {
    expect(useAddToCart(ref(product(3))).maxQuantity.value).toBe(3)
    expect(useAddToCart(ref(product(500))).maxQuantity.value).toBe(MAX_QUANTITY_PER_ADD)
  })
})
