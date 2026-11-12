import { storeToRefs } from 'pinia'
import { useCartStore } from '@/stores/cart.store'

export function useCart() {
  const store = useCartStore()
  const { items, totalItems, totalPrice } = storeToRefs(store)
  return {
    items,
    totalItems,
    totalPrice,
    fetchCart: store.fetchCart,
    addItem: store.addItem,
    updateItem: store.updateItem,
    removeItem: store.removeItem,
    clearCart: store.clearCart,
  }
}
