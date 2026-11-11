import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cartService } from '@/features/cart/services/cart.service'
import type { CartItem } from '@/types/cart.types'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const totalItems = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalPrice = computed(() => items.value.reduce((sum, item) => sum + item.product.price * item.quantity, 0))

  async function fetchCart() {
    items.value = await cartService.get()
  }

  async function addItem(productId: number, quantity = 1) {
    items.value = await cartService.addItem(productId, quantity)
  }

  return { items, totalItems, totalPrice, fetchCart, addItem }
})
