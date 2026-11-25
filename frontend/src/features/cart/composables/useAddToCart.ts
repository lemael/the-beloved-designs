import { computed, ref, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'
import { errorMessage } from '@/utils/api-error'
import type { Product } from '@/types/product.types'

/** Plafond par ajout : évite les quantités aberrantes même si le stock est grand. */
export const MAX_QUANTITY_PER_ADD = 10

export type AddToCartStatus = 'idle' | 'adding' | 'added' | 'error'

/**
 * Logique du bouton « Ajouter au panier » de la fiche produit :
 * - visiteur non connecté → page de connexion, puis retour sur la fiche ;
 * - un seul envoi à la fois (pas de double ajout sur double clic) ;
 * - succès / erreur exposés pour l'affichage (le message d'erreur est celui du serveur, ex. stock).
 */
export function useAddToCart(product: Ref<Product | null>) {
  const { isAuthenticated } = useAuth()
  const { addItem } = useCart()
  const router = useRouter()
  const route = useRoute()

  const quantity = ref(1)
  const status = ref<AddToCartStatus>('idle')
  const error = ref<string | null>(null)

  const inStock = computed(() => (product.value?.stockQuantity ?? 0) > 0)
  const maxQuantity = computed(() =>
    Math.max(1, Math.min(product.value?.stockQuantity ?? 1, MAX_QUANTITY_PER_ADD)),
  )

  // Autre produit affiché (navigation d'une fiche à l'autre) : on repart de zéro.
  watch(
    () => product.value?.id,
    () => {
      quantity.value = 1
      status.value = 'idle'
      error.value = null
    },
  )

  async function add() {
    if (!product.value || !inStock.value || status.value === 'adding') return

    if (!isAuthenticated.value) {
      await router.push({ path: '/account', query: { redirect: route.fullPath } })
      return
    }

    status.value = 'adding'
    error.value = null
    try {
      await addItem(product.value.id, quantity.value)
      status.value = 'added'
    } catch (e) {
      status.value = 'error'
      error.value = errorMessage(e, "Impossible d'ajouter ce produit au panier. Réessayez dans un instant.")
    }
  }

  return { quantity, status, error, inStock, maxQuantity, add }
}
