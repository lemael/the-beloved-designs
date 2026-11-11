<script setup lang="ts">
import type { Product } from '@/types/product.types'
import ProductCard from './ProductCard.vue'

defineProps<{ products: Product[] }>()
</script>

<template>
  <div class="product-carousel">
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

.product-carousel__item {
  flex: 0 0 200px;
  scroll-snap-align: start;
}
</style>
