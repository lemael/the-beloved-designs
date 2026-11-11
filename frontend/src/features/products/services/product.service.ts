import { api } from '@/services/api'
import type { Page, Product } from '@/types/product.types'

export const productService = {
  async list(params: { category?: string; page?: number; size?: number } = {}): Promise<Page<Product>> {
    const { data } = await api.get<Page<Product>>('/products', { params })
    return data
  },

  async featured(params: { page?: number; size?: number } = {}): Promise<Page<Product>> {
    const { data } = await api.get<Page<Product>>('/products/featured', { params })
    return data
  },

  async getBySlug(slug: string): Promise<Product> {
    const { data } = await api.get<Product>(`/products/${slug}`)
    return data
  },
}
