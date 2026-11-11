import { api } from '@/services/api'
import type { Order } from '@/types/order.types'
import type { Page } from '@/types/product.types'

export const orderService = {
  async list(params: { page?: number; size?: number } = {}): Promise<Page<Order>> {
    const { data } = await api.get<Page<Order>>('/orders', { params })
    return data
  },

  async getById(id: number): Promise<Order> {
    const { data } = await api.get<Order>(`/orders/${id}`)
    return data
  },
}
