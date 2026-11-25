import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cartService } from '@/features/cart/services/cart.service'
import type { Cart } from '@/types/cart.types'

/**
 * Panier du client connecté. Le serveur fait foi : après chaque modification, on recharge le panier
 * complet (prix, promo, total calculés côté serveur). Aucun prix n'est recalculé ici.
 */
export const useCartStore = defineStore('cart', () => {
  const cart = ref<Cart | null>(null)

  const items = computed(() => cart.value?.items ?? [])
  const totalItems = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalPrice = computed(() => cart.value?.total ?? 0)

  async function fetchCart() {
    cart.value = await cartService.get()
  }

  /** Ajoute au panier puis recharge le panier. L'erreur (stock, connexion…) remonte à l'appelant. */
  async function addItem(productId: number, quantity = 1) {
    await cartService.addItem(productId, quantity)
    await fetchCart()
  }

  async function updateItem(itemId: number, quantity: number) {
    await cartService.updateItem(itemId, quantity)
    await fetchCart()
  }

  async function removeItem(itemId: number) {
    await cartService.removeItem(itemId)
    await fetchCart()
  }

  async function clearCart() {
    await cartService.clearCart()
    await fetchCart()
  }

  /** Déconnexion : on oublie le panier du client précédent. */
  function reset() {
    cart.value = null
  }

  return { items, totalItems, totalPrice, fetchCart, addItem, updateItem, removeItem, clearCart, reset }
})
