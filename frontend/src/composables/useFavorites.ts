import { storeToRefs } from 'pinia'
import { useFavoritesStore } from '@/stores/favorites.store'

export function useFavorites() {
  const store = useFavoritesStore()
  const { items } = storeToRefs(store)
  return { items, fetchFavorites: store.fetchFavorites, toggle: store.toggle }
}
