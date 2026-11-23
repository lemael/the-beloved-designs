import type { Product } from './product.types'

/** Promotion en cours, renvoyée par GET /api/promotions avec ses produits visibles sur le site. */
export interface Promotion {
  id: number
  label: string
  discountPercent: number
  startsAt: string | null
  endsAt: string | null
  isActive: boolean
  products: Product[]
}
