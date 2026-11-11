import { ref } from 'vue'
import { productService } from '@/features/products/services/product.service'
import type { Product } from '@/types/product.types'

export function useProducts() {
  const products = ref<Product[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function fetchFeatured() {
    isLoading.value = true
    error.value = null
    try {
      const page = await productService.featured()
      products.value = page.content
    } catch {
      error.value = 'Impossible de charger les produits pour le moment.'
    } finally {
      isLoading.value = false
    }
  }

  return { products, isLoading, error, fetchFeatured }
}
