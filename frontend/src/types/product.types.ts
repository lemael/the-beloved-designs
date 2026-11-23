export interface Product {
  id: number
  name: string
  slug: string
  description: string | null
  price: number
  currency: string
  stockQuantity: number
  featured: boolean
  imageUrls: string[]
  categorySlugs: string[]
  /** Réduction en cours (null si le produit n'est pas en promotion) */
  discountPercent: number | null
  /** Prix après réduction (null si pas de promotion) ; price reste le prix normal */
  promoPrice: number | null
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
