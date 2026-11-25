export type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export interface OrderItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface OrderDelivery {
  city: string
  district: string
  /** Numéro WhatsApp que le livreur appellera */
  contactPhone: string
}

/** Commande : tous les montants sont calculés par le serveur. */
export interface Order {
  id: number
  status: OrderStatus
  subtotal: number
  firstOrderDiscount: number
  deliveryFee: number
  totalAmount: number
  currency: string
  createdAt: string
  delivery: OrderDelivery
  items: OrderItem[]
  /** Lien wa.me vers la boutique avec le détail pré-rempli (null si la boutique n'a pas configuré son numéro) */
  whatsappUrl: string | null
}

export interface PlaceOrderPayload {
  deliveryCity: string
  deliveryDistrict: string
  contactPhone: string
}
