import { api } from '@/services/api'
import type { Cart, CartItem } from '@/types/cart.types'

export const cartService = {
  async get(): Promise<Cart> {
    const { data } = await api.get<Cart>('/cart')
    return data
  },

  async addItem(productId: number, quantity: number): Promise<CartItem> {
    const { data } = await api.post<CartItem>('/cart/items', { productId, quantity })
    return data
  },

  async updateItem(itemId: number, quantity: number): Promise<CartItem> {
    const { data } = await api.put<CartItem>(`/cart/items/${itemId}`, { quantity })
    return data
  },

  async removeItem(itemId: number): Promise<void> {
    await api.delete(`/cart/items/${itemId}`)
  },

  async clearCart(): Promise<void> {
    await api.delete('/cart')
  },
}
