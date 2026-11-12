import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cartService } from '@/features/cart/services/cart.service'
import type { Cart, CartItem } from '@/types/cart.types'

export const useCartStore = defineStore('cart', () => {
  const cart = ref<Cart | null>(null)

  const items = computed(() => cart.value?.items ?? [])
  const totalItems = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalPrice = computed(() => cart.value?.total ?? 0)

  async function fetchCart() {
    cart.value = await cartService.get()
  }

  async function addItem(productId: number, quantity = 1) {
    const newItem = await cartService.addItem(productId, quantity)
    if (cart.value) {
      const existing = cart.value.items.find(item => item.id === newItem.id)
      if (existing) {
        existing.quantity = newItem.quantity
      } else {
        cart.value.items.push(newItem)
      }
      // Recalculate total
      cart.value.total = cart.value.items.reduce(
        (sum, item) => sum + item.product.price * item.quantity,
        0
      )
    }
  }

  async function updateItem(itemId: number, quantity: number) {
    const updatedItem = await cartService.updateItem(itemId, quantity)
    if (cart.value) {
      const item = cart.value.items.find(i => i.id === itemId)
      if (item) {
        item.quantity = updatedItem.quantity
      }
      // Recalculate total
      cart.value.total = cart.value.items.reduce(
        (sum, item) => sum + item.product.price * item.quantity,
        0
      )
    }
  }

  async function removeItem(itemId: number) {
    await cartService.removeItem(itemId)
    if (cart.value) {
      cart.value.items = cart.value.items.filter(item => item.id !== itemId)
      // Recalculate total
      cart.value.total = cart.value.items.reduce(
        (sum, item) => sum + item.product.price * item.quantity,
        0
      )
    }
  }

  async function clearCart() {
    await cartService.clearCart()
    if (cart.value) {
      cart.value.items = []
      cart.value.total = 0
    }
  }

  return { items, totalItems, totalPrice, fetchCart, addItem, updateItem, removeItem, clearCart }
})

