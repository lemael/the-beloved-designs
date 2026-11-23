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
