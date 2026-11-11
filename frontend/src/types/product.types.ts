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
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
