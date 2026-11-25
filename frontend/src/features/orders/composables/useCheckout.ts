import { computed, reactive, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'
import { orderService } from '@/features/orders/services/order.service'
import { errorMessage, toApiError } from '@/utils/api-error'
import { isValidPhone } from '@/utils/validators'
import type { Order } from '@/types/order.types'

export type CheckoutStatus = 'editing' | 'submitting' | 'placed' | 'error'

/**
 * Page « Finaliser la commande » : formulaire de livraison (ville, quartier, numéro WhatsApp du
 * livreur), envoi unique, puis commande confirmée avec le lien WhatsApp vers la boutique.
 * Les montants (réduction, livraison, total) viennent toujours du serveur.
 */
export function useCheckout() {
  const { user } = useAuth()
  const { fetchCart } = useCart()

  const form = reactive({
    deliveryCity: '',
    deliveryDistrict: '',
    // Pré-rempli avec le numéro du compte ; le client peut en donner un autre pour le livreur
    contactPhone: user.value?.phone ?? '',
  })
  const fieldErrors = reactive<{ deliveryCity?: string; deliveryDistrict?: string; contactPhone?: string }>({})
  const status = ref<CheckoutStatus>('editing')
  const error = ref<string | null>(null)
  /** Le panier a changé côté serveur (stock, produit retiré) : inviter à le revoir */
  const cartNeedsReview = ref(false)
  const order = ref<Order | null>(null)

  const isSubmitting = computed(() => status.value === 'submitting')

  function validate(): boolean {
    fieldErrors.deliveryCity = form.deliveryCity.trim() ? undefined : 'Indiquez la ville de livraison.'
    fieldErrors.deliveryDistrict = form.deliveryDistrict.trim() ? undefined : 'Indiquez le quartier.'
    fieldErrors.contactPhone = isValidPhone(form.contactPhone.trim())
      ? undefined
      : 'Numéro WhatsApp au format international, ex. +237612345678.'
    return !fieldErrors.deliveryCity && !fieldErrors.deliveryDistrict && !fieldErrors.contactPhone
  }

  async function submit() {
    if (isSubmitting.value || status.value === 'placed') return
    if (!validate()) return

    status.value = 'submitting'
    error.value = null
    cartNeedsReview.value = false
    try {
      order.value = await orderService.place({
        deliveryCity: form.deliveryCity.trim(),
        deliveryDistrict: form.deliveryDistrict.trim(),
        contactPhone: form.contactPhone.trim(),
      })
      status.value = 'placed'
      // Le serveur a vidé le panier : le compteur de l'en-tête doit repasser à 0
      fetchCart().catch(() => {})
    } catch (e) {
      status.value = 'error'
      const code = toApiError(e)?.code
      cartNeedsReview.value = code === 'INSUFFICIENT_STOCK' || code === 'PRODUCT_UNAVAILABLE' || code === 'EMPTY_CART'
      error.value = errorMessage(e, "La commande n'a pas pu être envoyée. Vérifiez votre connexion et réessayez.")
    }
  }

  return { form, fieldErrors, status, error, cartNeedsReview, order, isSubmitting, submit }
}
