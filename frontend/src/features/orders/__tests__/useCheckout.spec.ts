import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError, AxiosHeaders } from 'axios'
import { useCheckout } from '@/features/orders/composables/useCheckout'
import { orderService } from '@/features/orders/services/order.service'
import { useAuthStore } from '@/stores/auth.store'
import { useCartStore } from '@/stores/cart.store'
import type { Order } from '@/types/order.types'

vi.mock('@/features/orders/services/order.service', () => ({
  orderService: { place: vi.fn(), list: vi.fn(), getById: vi.fn() },
}))

const placed: Order = {
  id: 42,
  status: 'PENDING',
  subtotal: 30000,
  firstOrderDiscount: 3000,
  deliveryFee: 1500,
  totalAmount: 28500,
  currency: 'FCFA',
  createdAt: '2026-11-25T20:00:00Z',
  delivery: { city: 'Douala', district: 'Akwa', contactPhone: '+237699112233' },
  items: [{ productId: 7, productName: 'Robe', quantity: 2, unitPrice: 15000, lineTotal: 30000 }],
  whatsappUrl: 'https://wa.me/237600000001?text=Commande',
}

function apiError(status: number, code: string, message: string): AxiosError {
  const headers = new AxiosHeaders()
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', { headers }, null, {
    status,
    statusText: '',
    headers,
    config: { headers },
    data: { status, code, message },
  })
}

describe('useCheckout', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
    useAuthStore().user = { phone: '+237677000000', firstName: 'Awa', lastName: 'T', roles: ['CLIENT'] }
  })

  function fill(checkout: ReturnType<typeof useCheckout>) {
    checkout.form.deliveryCity = '  Douala '
    checkout.form.deliveryDistrict = 'Akwa'
    checkout.form.contactPhone = '+237699112233'
  }

  it('pré-remplit le numéro du livreur avec celui du compte', () => {
    expect(useCheckout().form.contactPhone).toBe('+237677000000')
  })

  it('champs manquants ou numéro invalide : messages par champ, aucun envoi', async () => {
    const checkout = useCheckout()
    checkout.form.contactPhone = '699112233'

    await checkout.submit()

    expect(checkout.fieldErrors.deliveryCity).toBeTruthy()
    expect(checkout.fieldErrors.deliveryDistrict).toBeTruthy()
    expect(checkout.fieldErrors.contactPhone).toBeTruthy()
    expect(orderService.place).not.toHaveBeenCalled()
  })

  it('commande réussie : envoie les champs nettoyés, garde la commande du serveur, recharge le panier', async () => {
    vi.mocked(orderService.place).mockResolvedValue(placed)
    const fetchCart = vi.spyOn(useCartStore(), 'fetchCart').mockResolvedValue()
    const checkout = useCheckout()
    fill(checkout)

    await checkout.submit()

    expect(orderService.place).toHaveBeenCalledWith({
      deliveryCity: 'Douala',
      deliveryDistrict: 'Akwa',
      contactPhone: '+237699112233',
    })
    expect(checkout.status.value).toBe('placed')
    expect(checkout.order.value?.whatsappUrl).toContain('https://wa.me/')
    expect(fetchCart).toHaveBeenCalled()   // compteur de l'en-tête → 0
  })

  it('double clic : une seule commande', async () => {
    let resolve!: (o: Order) => void
    vi.mocked(orderService.place).mockReturnValue(new Promise((r) => (resolve = r)))
    vi.spyOn(useCartStore(), 'fetchCart').mockResolvedValue()
    const checkout = useCheckout()
    fill(checkout)

    const first = checkout.submit()
    await checkout.submit()
    resolve(placed)
    await first
    await checkout.submit()   // déjà passée : ne recommande pas

    expect(orderService.place).toHaveBeenCalledTimes(1)
  })

  it('stock insuffisant : message du serveur + invitation à revoir le panier', async () => {
    vi.mocked(orderService.place).mockRejectedValue(
      apiError(409, 'INSUFFICIENT_STOCK', 'Stock insuffisant : plus que 1 disponible(s)'),
    )
    const checkout = useCheckout()
    fill(checkout)

    await checkout.submit()

    expect(checkout.status.value).toBe('error')
    expect(checkout.error.value).toBe('Stock insuffisant : plus que 1 disponible(s)')
    expect(checkout.cartNeedsReview.value).toBe(true)
  })

  it('erreur réseau : message de secours, pas d\'invitation à revoir le panier', async () => {
    vi.mocked(orderService.place).mockRejectedValue(new Error('Network Error'))
    const checkout = useCheckout()
    fill(checkout)

    await checkout.submit()

    expect(checkout.error.value).toContain("n'a pas pu être envoyée")
    expect(checkout.cartNeedsReview.value).toBe(false)
  })
})
