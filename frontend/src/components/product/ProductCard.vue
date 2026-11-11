<script setup lang="ts">
import type { Product } from '@/types/product.types'
import ProductPrice from './ProductPrice.vue'
import ProductBadge from './ProductBadge.vue'

defineProps<{ product: Product }>()
</script>

<template>
  <RouterLink :to="`/products/${product.slug}`" class="product-card">
    <div class="product-card__image">
      <img v-if="product.imageUrls[0]" :src="product.imageUrls[0]" :alt="product.name" />
      <ProductBadge class="product-card__badge" :featured="product.featured" />
    </div>
    <h3 class="product-card__name">{{ product.name }}</h3>
    <ProductPrice :price="product.price" :currency="product.currency" />
  </RouterLink>
</template>

<style scoped>
.product-card {
  display: block;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-sm);
  color: var(--color-text);
}

.product-card:hover {
  border-color: var(--color-gold);
}

.product-card__image {
  position: relative;
  aspect-ratio: 3 / 4;
  background-color: #000;
  border-radius: var(--radius-sm);
  overflow: hidden;
  margin-bottom: var(--spacing-sm);
}

.product-card__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-card__badge {
  position: absolute;
  top: var(--spacing-sm);
  left: var(--spacing-sm);
}

.product-card__name {
  font-size: 0.95rem;
  margin: 0 0 var(--spacing-xs);
}
</style>
