<script setup lang="ts">
import { ref } from 'vue'
import type { Product } from '@/types/product.types'
import ProductCard from './ProductCard.vue'

defineProps<{ products: Product[] }>()

const track = ref<HTMLElement | null>(null)

/** Fait défiler le carrousel d'une « page » (utilisé par les flèches de la page d'accueil). */
function scrollPage(direction: 1 | -1) {
  const el = track.value
  if (!el) return
  el.scrollBy({ left: direction * el.clientWidth * 0.9, behavior: 'smooth' })
}

defineExpose({ scrollPage })
</script>

<template>
  <div ref="track" class="product-carousel">
    <ProductCard v-for="product in products" :key="product.id" :product="product" class="product-carousel__item" />
  </div>
</template>

<style scoped>
.product-carousel {
  display: flex;
  gap: var(--spacing-md);
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: var(--spacing-sm);
}

.product-carousel {
  scrollbar-width: none;
}

.product-carousel::-webkit-scrollbar {
  display: none;
}

.product-carousel__item {
  flex: 0 0 200px;
  scroll-snap-align: start;
}
</style>
