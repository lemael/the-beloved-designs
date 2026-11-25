import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCartStore } from '@/stores/cart.store'
import { cartService } from '@/features/cart/services/cart.service'
import type { Cart } from '@/types/cart.types'
import type { Product } from '@/types/product.types'

vi.mock('@/features/cart/services/cart.service', () => ({
  cartService: {
    get: vi.fn(),
    addItem: vi.fn(),
    updateItem: vi.fn(),
    removeItem: vi.fn(),
    clearCart: vi.fn(),
  },
}))

const product = { id: 7, name: 'Robe', price: 10000, promoPrice: 9000, currency: 'FCFA' } as Product

/** Panier tel que renvoyé par le serveur : prix promo et total déjà calculés. */
function serverCart(quantity: number): Cart {
  return {
    id: 1,
    items: [{ id: 3, product, quantity, unitPrice: 9000, lineTotal: 9000 * quantity }],
    total: 9000 * quantity,
  }
}

describe('cart.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it("ajout alors que le panier n'avait jamais été chargé : le compteur et le total suivent le serveur", async () => {
    vi.mocked(cartService.addItem).mockResolvedValue({} as never)
    vi.mocked(cartService.get).mockResolvedValue(serverCart(2))
    const store = useCartStore()

    await store.addItem(7, 2)

    expect(cartService.addItem).toHaveBeenCalledWith(7, 2)
    expect(store.totalItems).toBe(2)
    expect(store.totalPrice).toBe(18000) // prix promo du serveur, pas 2 × 10 000
  })

  it("ajout refusé (stock) : l'erreur remonte et le panier reste inchangé", async () => {
    vi.mocked(cartService.get).mockResolvedValue(serverCart(1))
    const store = useCartStore()
    await store.fetchCart()
    vi.mocked(cartService.addItem).mockRejectedValue(new Error('409'))

    await expect(store.addItem(7, 5)).rejects.toThrow('409')

    expect(store.totalItems).toBe(1)
    expect(cartService.get).toHaveBeenCalledTimes(1) // pas de rechargement après un échec
  })

  it('modification et suppression rechargent le panier du serveur', async () => {
    vi.mocked(cartService.get).mockResolvedValueOnce(serverCart(3)).mockResolvedValueOnce({ id: 1, items: [], total: 0 })
    const store = useCartStore()

    await store.updateItem(3, 3)
    expect(store.totalItems).toBe(3)

    await store.removeItem(3)
    expect(store.totalItems).toBe(0)
    expect(store.totalPrice).toBe(0)
  })

  it('reset (déconnexion) oublie le panier', async () => {
    vi.mocked(cartService.get).mockResolvedValue(serverCart(2))
    const store = useCartStore()
    await store.fetchCart()

    store.reset()

    expect(store.totalItems).toBe(0)
    expect(store.items).toEqual([])
  })
})
