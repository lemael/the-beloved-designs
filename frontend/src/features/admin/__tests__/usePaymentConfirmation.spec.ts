import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { todayIso, usePaymentConfirmation } from '@/features/admin/composables/usePaymentConfirmation'
import { adminService } from '@/features/admin/services/admin.service'
import { productService } from '@/features/products/services/product.service'
import type { OrderPaymentPrefill, PaymentConfirmation } from '@/types/admin.types'
import type { Product } from '@/types/product.types'

vi.mock('@/features/admin/services/admin.service', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/features/admin/services/admin.service')>()
  return {
    ...original,
    adminService: { paymentPrefill: vi.fn(), deliveryFeeQuote: vi.fn(), confirmPayment: vi.fn() },
  }
})
vi.mock('@/features/products/services/product.service', () => ({ productService: { list: vi.fn() } }))
vi.mock('@/features/products/services/category.service', () => ({ categoryService: { list: vi.fn() } }))

const robe = { id: 7, name: 'Robe', price: 15000, promoPrice: null, stockQuantity: 5 } as Product
const sac = { id: 9, name: 'Sac', price: 25000, promoPrice: 20000, stockQuantity: 4 } as Product

const siteOrder: OrderPaymentPrefill = {
  orderId: 12,
  status: 'PENDING',
  customerWhatsapp: '+237699112233',
  deliveryCity: 'Douala',
  deliveryDistrict: 'Akwa',
  deliveryFee: 1500,
  subtotal: 30000,
  firstOrderDiscount: 3000,
  total: 28500,
  items: [{ productId: 7, productName: 'Robe', categorySlug: 'femmes', quantity: 2, unitPrice: 15000 }],
}

describe('usePaymentConfirmation', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
    vi.mocked(productService.list).mockImplementation(async ({ category } = {}) => ({
      content: category === 'femmes' ? [robe] : [sac],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 200,
    }))
  })

  it('date du paiement : aujourd’hui à l’heure locale, au format AAAA-MM-JJ', () => {
    expect(todayIso(new Date(2026, 10, 5, 23, 30))).toBe('2026-11-05')
    expect(usePaymentConfirmation().form.paidOn).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('n° de commande : formulaire pré-rempli (client, produits, livraison, montant payé)', async () => {
    vi.mocked(adminService.paymentPrefill).mockResolvedValue(siteOrder)
    const f = usePaymentConfirmation()
    f.orderNumber.value = 'n° 12'

    await f.loadOrder()

    expect(adminService.paymentPrefill).toHaveBeenCalledWith(12)
    expect(f.form.customerWhatsapp).toBe('+237699112233')
    expect(f.form.lines).toHaveLength(1)
    expect(f.form.lines[0]).toMatchObject({ categorySlug: 'femmes', productId: 7, quantity: 2 })
    expect(f.form.deliveryFee).toBe(1500)
    expect(f.subtotal.value).toBe(30000)
    expect(f.firstOrderDiscount.value).toBe(3000)
    expect(f.total.value).toBe(28500)
    expect(f.form.amountPaid).toBe(28500)
  })

  it('commande déjà payée : avertissement', async () => {
    vi.mocked(adminService.paymentPrefill).mockResolvedValue({ ...siteOrder, status: 'PAID' })
    const f = usePaymentConfirmation()
    f.orderNumber.value = '12'

    await f.loadOrder()

    expect(f.orderError.value).toContain("n'est plus en attente")
  })

  it('ligne ajoutée avec « + » : prix du jour (promo) ; -10 % recalculé ; montant payé suit le total', async () => {
    vi.mocked(adminService.paymentPrefill).mockResolvedValue(siteOrder)
    const f = usePaymentConfirmation()
    f.orderNumber.value = '12'
    await f.loadOrder()

    f.addLine()
    const line = f.form.lines[1]!
    await f.changeCategory(line, 'sacs-a-main')
    line.productId = 9
    await nextTick()

    expect(f.unitPrice(line)).toBe(20000)            // prix promo du jour
    expect(f.subtotal.value).toBe(50000)             // 30 000 + 20 000
    expect(f.firstOrderDiscount.value).toBe(5000)
    expect(f.total.value).toBe(46500)                // 50 000 - 5 000 + 1 500
    expect(f.form.amountPaid).toBe(46500)
  })

  it('montant payé saisi à la main : plus écrasé par le total', async () => {
    const f = usePaymentConfirmation()
    await f.changeCategory(f.form.lines[0]!, 'femmes')
    f.form.lines[0]!.productId = 7
    await nextTick()
    f.form.amountPaid = 14000
    f.touched.amountPaid = true

    f.form.discount = 500
    await nextTick()

    expect(f.form.amountPaid).toBe(14000)
  })

  it('vente directe : envoie orderId null et les lignes remplies seulement', async () => {
    vi.mocked(adminService.confirmPayment).mockResolvedValue({ orderId: 30 } as PaymentConfirmation)
    const f = usePaymentConfirmation()
    f.form.customerWhatsapp = ' +237677000000 '
    await f.changeCategory(f.form.lines[0]!, 'femmes')
    f.form.lines[0]!.productId = 7
    f.form.lines[0]!.quantity = 2
    f.addLine()                                      // ligne vide : ignorée
    f.form.deliveryCity = 'Yaoundé'
    f.form.deliveryFee = 2000
    f.form.discount = 1000
    f.form.amountPaid = 31000
    f.form.paymentMethod = 'ORANGE_MONEY'
    f.form.paidOn = '2026-11-20'

    await f.submit()

    expect(adminService.confirmPayment).toHaveBeenCalledWith({
      orderId: null,
      customerWhatsapp: '+237677000000',
      items: [{ productId: 7, quantity: 2 }],
      deliveryCity: 'Yaoundé',
      deliveryDistrict: null,
      deliveryFee: 2000,
      discount: 1000,
      amountPaid: 31000,
      paymentMethod: 'ORANGE_MONEY',
      paidOn: '2026-11-20',
    })
    expect(f.status.value).toBe('done')
    expect(f.result.value?.orderId).toBe(30)
  })

  it('formulaire incomplet : message, aucun envoi', async () => {
    const f = usePaymentConfirmation()

    f.form.customerWhatsapp = '699112233'
    await f.submit()
    expect(f.error.value).toContain('WhatsApp')

    f.form.customerWhatsapp = '+237699112233'
    await f.submit()
    expect(f.error.value).toContain('au moins un produit')

    await f.changeCategory(f.form.lines[0]!, 'femmes')
    f.form.lines[0]!.productId = 7
    await f.submit()
    expect(f.error.value).toContain('mode de paiement')

    expect(adminService.confirmPayment).not.toHaveBeenCalled()
  })

  it('« Nouvelle confirmation » vide le formulaire', async () => {
    vi.mocked(adminService.paymentPrefill).mockResolvedValue(siteOrder)
    const f = usePaymentConfirmation()
    f.orderNumber.value = '12'
    await f.loadOrder()

    f.reset()

    expect(f.loadedOrder.value).toBeNull()
    expect(f.form.customerWhatsapp).toBe('')
    expect(f.form.lines).toHaveLength(1)
    expect(f.form.lines[0]!.productId).toBeNull()
  })
})
