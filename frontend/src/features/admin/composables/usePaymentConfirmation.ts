import { computed, reactive, ref, watch } from 'vue'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import { categoryService } from '@/features/products/services/category.service'
import { productService } from '@/features/products/services/product.service'
import { isValidPhone } from '@/utils/validators'
import type { Category } from '@/types/category.types'
import type { Product } from '@/types/product.types'
import type { OrderPaymentPrefill, PaymentConfirmation, PaymentMethod } from '@/types/admin.types'

/** Produits proposés par catégorie (catalogue public : produits en vente, prix promo compris). */
const PRODUCTS_PER_CATEGORY = 200
/** Même règle que le serveur pour l'estimation affichée (le serveur fait foi). */
const FIRST_ORDER_DISCOUNT_PERCENT = 10

export interface PaymentLine {
  key: number
  categorySlug: string | null
  productId: number | null
  quantity: number
}

/** Date du jour au format AAAA-MM-JJ, à l'heure locale (pas UTC). */
export function todayIso(now = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

/**
 * Formulaire « Confirmation paiement » du back-office (décision du 2026-11-25) :
 * - n° de commande du site (facultatif) → formulaire pré-rempli ; sans n° → vente directe ;
 * - lignes produit ajoutées avec « + » : catégorie → produit → quantité ;
 * - livraison : tarif de la ville proposé (modifiable) ; réduction manuelle ; montant payé
 *   pré-rempli avec le total (modifiable) ; mode et date du paiement ;
 * - après confirmation : lien WhatsApp du reçu vers le client.
 * Les montants affichés pendant la saisie sont une estimation ; ceux du serveur font foi.
 */
export function usePaymentConfirmation() {
  let nextKey = 1
  const newLine = (): PaymentLine => ({ key: nextKey++, categorySlug: null, productId: null, quantity: 1 })

  // --- Catalogue ---
  const categories = ref<Category[]>([])
  const productsByCategory = reactive<Record<string, Product[]>>({})

  async function loadCategories() {
    if (categories.value.length) return
    categories.value = await categoryService.list()
  }

  async function loadProducts(slug: string | null) {
    if (!slug || productsByCategory[slug]) return
    productsByCategory[slug] = (await productService.list({ category: slug, size: PRODUCTS_PER_CATEGORY })).content
  }

  // --- Commande du site ---
  const orderNumber = ref('')
  const loadedOrder = ref<OrderPaymentPrefill | null>(null)
  const loadingOrder = ref(false)
  const orderError = ref<string | null>(null)

  // --- Formulaire ---
  const form = reactive({
    customerWhatsapp: '',
    lines: [newLine()] as PaymentLine[],
    deliveryCity: '',
    deliveryDistrict: '',
    deliveryFee: 0,
    discount: 0,
    amountPaid: 0,
    paymentMethod: null as PaymentMethod | null,
    paidOn: todayIso(),
  })
  /** L'admin a tapé lui-même ces montants : on ne les écrase plus automatiquement. */
  const touched = reactive({ deliveryFee: false, amountPaid: false })

  const status = ref<'editing' | 'submitting' | 'done'>('editing')
  const error = ref<string | null>(null)
  const result = ref<PaymentConfirmation | null>(null)

  function productOf(line: PaymentLine): Product | undefined {
    if (!line.categorySlug || line.productId === null) return undefined
    return productsByCategory[line.categorySlug]?.find((p) => p.id === line.productId)
  }

  /** Prix d'une ligne : celui convenu à la commande si le produit y était, sinon le prix du jour. */
  function unitPrice(line: PaymentLine): number {
    const fromOrder = loadedOrder.value?.items.find((i) => i.productId === line.productId)
    if (fromOrder) return fromOrder.unitPrice
    const product = productOf(line)
    return product ? (product.promoPrice ?? product.price) : 0
  }

  const filledLines = computed(() => form.lines.filter((l) => l.productId !== null && l.quantity > 0))
  const subtotal = computed(() => filledLines.value.reduce((sum, l) => sum + unitPrice(l) * l.quantity, 0))
  const firstOrderDiscount = computed(() =>
    loadedOrder.value && loadedOrder.value.firstOrderDiscount > 0
      ? Math.round((subtotal.value * FIRST_ORDER_DISCOUNT_PERCENT) / 100)
      : 0,
  )
  const total = computed(
    () => subtotal.value - firstOrderDiscount.value - (form.discount || 0) + (form.deliveryFee || 0),
  )

  // Montant payé = total, tant que l'admin ne l'a pas modifié
  watch(total, (value) => {
    if (!touched.amountPaid) form.amountPaid = Math.max(0, value)
  })

  // Tarif de livraison proposé quand la ville ou le montant change (sauf saisie manuelle)
  let quoteTimer: ReturnType<typeof setTimeout> | undefined
  watch(
    () => [form.deliveryCity, subtotal.value - firstOrderDiscount.value - (form.discount || 0)] as const,
    ([city, amount]) => {
      if (touched.deliveryFee || !city.trim()) return
      clearTimeout(quoteTimer)
      quoteTimer = setTimeout(async () => {
        try {
          const fee = await adminService.deliveryFeeQuote(city.trim(), Math.max(0, amount))
          if (!touched.deliveryFee) form.deliveryFee = fee
        } catch {
          // Pas bloquant : l'admin peut saisir le montant lui-même
        }
      }, 300)
    },
  )

  async function loadOrder() {
    orderError.value = null
    const id = Number(orderNumber.value.trim().replace(/^n°\s*/i, ''))
    if (!Number.isInteger(id) || id <= 0) {
      orderError.value = 'Saisissez un numéro de commande (ex. 12).'
      return
    }
    loadingOrder.value = true
    try {
      const order = await adminService.paymentPrefill(id)
      await Promise.all(order.items.map((i) => loadProducts(i.categorySlug)))
      form.customerWhatsapp = order.customerWhatsapp
      form.lines = order.items.map((i) => ({
        key: nextKey++,
        categorySlug: i.categorySlug,
        productId: i.productId,
        quantity: i.quantity,
      }))
      if (form.lines.length === 0) form.lines = [newLine()]
      form.deliveryCity = order.deliveryCity ?? ''
      form.deliveryDistrict = order.deliveryDistrict ?? ''
      form.deliveryFee = order.deliveryFee
      touched.deliveryFee = true // repris de la commande : on ne le recalcule pas
      touched.amountPaid = false
      form.amountPaid = order.total
      // Annoncée en dernier : le message « commande chargée » n'apparaît qu'une fois le formulaire rempli
      loadedOrder.value = order
      if (order.status !== 'PENDING') {
        orderError.value = `Cette commande n'est plus en attente de paiement (statut : ${order.status}).`
      }
    } catch (e) {
      loadedOrder.value = null
      orderError.value = apiErrorMessage(e, 'Commande introuvable.')
    } finally {
      loadingOrder.value = false
    }
  }

  function addLine() {
    form.lines.push(newLine())
  }

  function removeLine(key: number) {
    form.lines = form.lines.filter((l) => l.key !== key)
    if (form.lines.length === 0) form.lines = [newLine()]
  }

  async function changeCategory(line: PaymentLine, slug: string | null) {
    line.categorySlug = slug
    line.productId = null
    await loadProducts(slug)
  }

  function validate(): string | null {
    if (!isValidPhone(form.customerWhatsapp.trim())) {
      return 'Numéro WhatsApp du client au format international, ex. +237612345678.'
    }
    if (filledLines.value.length === 0) return 'Ajoutez au moins un produit.'
    if (form.lines.some((l) => l.productId !== null && (!Number.isInteger(l.quantity) || l.quantity < 1))) {
      return 'Chaque quantité doit être un nombre entier supérieur à 0.'
    }
    if (!form.paymentMethod) return 'Choisissez le mode de paiement.'
    if (!form.paidOn) return 'Indiquez la date du paiement.'
    if ([form.deliveryFee, form.discount, form.amountPaid].some((v) => v < 0 || Number.isNaN(v))) {
      return 'Les montants ne peuvent pas être négatifs.'
    }
    return null
  }

  async function submit() {
    if (status.value !== 'editing') return
    error.value = validate()
    if (error.value) return

    status.value = 'submitting'
    try {
      result.value = await adminService.confirmPayment({
        orderId: loadedOrder.value?.orderId ?? null,
        customerWhatsapp: form.customerWhatsapp.trim(),
        items: filledLines.value.map((l) => ({ productId: l.productId!, quantity: l.quantity })),
        deliveryCity: form.deliveryCity.trim() || null,
        deliveryDistrict: form.deliveryDistrict.trim() || null,
        deliveryFee: form.deliveryFee || 0,
        discount: form.discount || 0,
        amountPaid: form.amountPaid || 0,
        paymentMethod: form.paymentMethod!,
        paidOn: form.paidOn,
      })
      status.value = 'done'
    } catch (e) {
      status.value = 'editing'
      error.value = apiErrorMessage(e, "Le paiement n'a pas pu être enregistré.")
    }
  }

  /** Nouvelle confirmation (ou passage en vente directe) : formulaire vide. */
  function reset() {
    orderNumber.value = ''
    loadedOrder.value = null
    orderError.value = null
    Object.assign(form, {
      customerWhatsapp: '',
      lines: [newLine()],
      deliveryCity: '',
      deliveryDistrict: '',
      deliveryFee: 0,
      discount: 0,
      amountPaid: 0,
      paymentMethod: null,
      paidOn: todayIso(),
    })
    touched.deliveryFee = false
    touched.amountPaid = false
    status.value = 'editing'
    error.value = null
    result.value = null
  }

  return {
    categories,
    productsByCategory,
    loadCategories,
    orderNumber,
    loadedOrder,
    loadingOrder,
    orderError,
    loadOrder,
    form,
    touched,
    addLine,
    removeLine,
    changeCategory,
    productOf,
    unitPrice,
    subtotal,
    firstOrderDiscount,
    total,
    status,
    error,
    result,
    submit,
    reset,
  }
}
