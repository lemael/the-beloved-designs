import { api } from '@/services/api'
import type { Product } from '@/types/product.types'

export const favoriteService = {
  async list(): Promise<Product[]> {
    const { data } = await api.get<Product[]>('/favorites')
    return data
  },

  async add(productId: number): Promise<void> {
    await api.post(`/favorites/${productId}`)
  },

  async remove(productId: number): Promise<void> {
    await api.delete(`/favorites/${productId}`)
  },
}
