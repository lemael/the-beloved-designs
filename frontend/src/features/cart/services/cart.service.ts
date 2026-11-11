import { api } from '@/services/api'
import type { CartItem } from '@/types/cart.types'

export const cartService = {
  async get(): Promise<CartItem[]> {
    const { data } = await api.get<CartItem[]>('/cart')
    return data
  },

  async addItem(productId: number, quantity: number): Promise<CartItem[]> {
    const { data } = await api.post<CartItem[]>('/cart/items', { productId, quantity })
    return data
  },

  async updateItem(itemId: number, quantity: number): Promise<CartItem[]> {
    const { data } = await api.patch<CartItem[]>(`/cart/items/${itemId}`, { quantity })
    return data
  },

  async removeItem(itemId: number): Promise<void> {
    await api.delete(`/cart/items/${itemId}`)
  },
}
