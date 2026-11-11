import { defineStore } from 'pinia'
import { ref } from 'vue'
import { favoriteService } from '@/features/favorites/services/favorite.service'
import type { Product } from '@/types/product.types'

export const useFavoritesStore = defineStore('favorites', () => {
  const items = ref<Product[]>([])

  async function fetchFavorites() {
    items.value = await favoriteService.list()
  }

  async function toggle(productId: number) {
    const existing = items.value.find((p) => p.id === productId)
    if (existing) {
      await favoriteService.remove(productId)
      items.value = items.value.filter((p) => p.id !== productId)
    } else {
      await favoriteService.add(productId)
      await fetchFavorites()
    }
  }

  return { items, fetchFavorites, toggle }
})
