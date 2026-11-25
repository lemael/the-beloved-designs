import type { OrderStatus } from './order.types'

export interface AdminProductVariant {
  id: number
  size: string | null
  color: string | null
  stockQuantity: number
}

export interface AdminProduct {
  id: number
  name: string
  slug: string
  description: string | null
  price: number
  currency: string
  stockQuantity: number
  featured: boolean
  active: boolean
  imageUrls: string[]
  categorySlugs: string[]
  variants: AdminProductVariant[]
  createdAt: string
}

export interface CreateProductPayload {
  name: string
  slug?: string
  description?: string
  price: number
  stockQuantity: number
  featured: boolean
  active: boolean
  categorySlugs: string[]
  imageUrls: string[]
  variants: { size?: string; color?: string; stockQuantity: number }[]
}

export interface StockSummary {
  totalProducts: number
  totalUnits: number
  outOfStockCount: number
  lowStockCount: number
  lowStockThreshold: number
  byCategory: { slug: string; name: string; productCount: number; units: number; outOfStockCount: number }[]
  lowStockProducts: { id: number; name: string; slug: string; stockQuantity: number; active: boolean }[]
}

export type RestockStatus = 'PREVU' | 'RECU' | 'ANNULE'

export interface RestockItem {
  id: number
  productId: number
  productName: string
  variantId: number | null
  variantLabel: string | null
  quantity: number
  unitCost: number | null
}

export interface Restock {
  id: number
  expectedDate: string
  receivedAt: string | null
  supplier: string | null
  status: RestockStatus
  notes: string | null
  createdAt: string
  totalQuantity: number
  totalCost: number | null
  items: RestockItem[]
}

export interface CreateRestockPayload {
  expectedDate: string
  supplier?: string
  notes?: string
  items: { productId: number; variantId?: number | null; quantity: number; unitCost?: number | null }[]
}

export interface YearlyReport {
  year: number
  revenue: number
  orderCount: number
  averageOrderValue: number
  currency: string
  months: { month: number; orderCount: number; revenue: number }[]
  topProducts: { productId: number; productName: string; quantity: number; revenue: number }[]
  byCategory: { slug: string; name: string; quantity: number; revenue: number }[]
}

export interface AdminDashboard {
  totalProducts: number
  outOfStockCount: number
  lowStockCount: number
  pendingRestocks: number
  currentYear: number
  revenueThisYear: number
  ordersThisYear: number
  currency: string
}

// ---- Confirmation paiement (décision du 2026-11-25) ----

export type PaymentMethod = 'CASH' | 'MTN_MOMO' | 'ORANGE_MONEY' | 'BANK_TRANSFER'

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Espèces',
  MTN_MOMO: 'MTN Mobile Money',
  ORANGE_MONEY: 'Orange Money',
  BANK_TRANSFER: 'Virement bancaire',
}

/** Commande du site chargée dans le formulaire (saisie du n° de commande). */
export interface OrderPaymentPrefill {
  orderId: number
  status: OrderStatus
  customerWhatsapp: string
  deliveryCity: string | null
  deliveryDistrict: string | null
  deliveryFee: number
  subtotal: number
  firstOrderDiscount: number
  total: number
  items: { productId: number; productName: string; categorySlug: string | null; quantity: number; unitPrice: number }[]
}

export interface ConfirmPaymentPayload {
  /** Absent : vente directe */
  orderId: number | null
  customerWhatsapp: string
  items: { productId: number; quantity: number }[]
  deliveryCity: string | null
  deliveryDistrict: string | null
  deliveryFee: number
  discount: number
  amountPaid: number
  paymentMethod: PaymentMethod
  /** AAAA-MM-JJ */
  paidOn: string
}

export interface PaymentConfirmation {
  orderId: number
  paymentId: number
  status: OrderStatus
  customerWhatsapp: string
  deliveryCity: string | null
  deliveryDistrict: string | null
  items: { productId: number; productName: string; quantity: number; unitPrice: number; lineTotal: number }[]
  subtotal: number
  firstOrderDiscount: number
  discount: number
  deliveryFee: number
  total: number
  amountPaid: number
  paymentMethod: PaymentMethod
  paidOn: string
  /** Lien wa.me vers le WhatsApp du client, reçu pré-rempli */
  receiptWhatsappUrl: string
}
