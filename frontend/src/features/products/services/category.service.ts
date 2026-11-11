import { api } from '@/services/api'
import type { Category } from '@/types/category.types'

export const categoryService = {
  async list(): Promise<Category[]> {
    const { data } = await api.get<Category[]>('/categories')
    return data
  },

  async getBySlug(slug: string): Promise<Category> {
    const { data } = await api.get<Category>(`/categories/${slug}`)
    return data
  },
}
