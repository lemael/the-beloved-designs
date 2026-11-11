import { api } from '@/services/api'
import type { Promotion } from '@/types/promotion.types'

export const promotionService = {
  async list(): Promise<Promotion[]> {
    const { data } = await api.get<Promotion[]>('/promotions')
    return data
  },
}
