<script setup lang="ts">
import { computed } from 'vue'
import { useFavorites } from '@/composables/useFavorites'

const props = defineProps<{ productId: number }>()
const { items, toggle } = useFavorites()

const isFavorite = computed(() => items.value.some((p) => p.id === props.productId))
</script>

<template>
  <button class="favorite-button" :class="{ active: isFavorite }" @click="toggle(productId)">
    {{ isFavorite ? '♥' : '♡' }}
  </button>
</template>

<style scoped>
.favorite-button {
  background: none;
  border: none;
  font-size: 1.25rem;
  cursor: pointer;
  color: var(--color-text-muted);
}

.favorite-button.active {
  color: var(--color-gold);
}
</style>
