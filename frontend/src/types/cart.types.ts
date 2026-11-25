import type { Product } from './product.types'

export interface CartItem {
  id: number
  product: Product
  quantity: number
  /** Prix unitaire facturé (promo comprise), fixé par le serveur à l'ajout. */
  unitPrice: number
  /** unitPrice × quantity, calculé par le serveur. */
  lineTotal: number
}

export interface Cart {
  id: number
  items: CartItem[]
  total: number
}
