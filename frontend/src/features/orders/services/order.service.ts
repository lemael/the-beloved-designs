import { api } from '@/services/api'
import type { Order, PlaceOrderPayload } from '@/types/order.types'
import type { Page } from '@/types/product.types'

export const orderService = {
  /** Passe la commande à partir du panier du client (stock décrémenté, panier vidé côté serveur). */
  async place(payload: PlaceOrderPayload): Promise<Order> {
    const { data } = await api.post<Order>('/orders', payload)
    return data
  },

  async list(params: { page?: number; size?: number } = {}): Promise<Page<Order>> {
    const { data } = await api.get<Page<Order>>('/orders', { params })
    return data
  },

  async getById(id: number): Promise<Order> {
    const { data } = await api.get<Order>(`/orders/${id}`)
    return data
  },
}
