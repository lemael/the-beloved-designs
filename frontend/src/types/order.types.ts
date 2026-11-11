export type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export interface Order {
  id: number
  status: OrderStatus
  totalAmount: number
  currency: string
  createdAt: string
}
