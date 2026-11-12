import type { Product } from './product.types'

export interface CartItem {
  id: number
  product: Product
  quantity: number
}

export interface Cart {
  id: number
  items: CartItem[]
  total: number
}
