<script setup lang="ts">
import { formatPrice } from '@/utils/currency'

defineProps<{ price: number; currency: string }>()
</script>

<template>
  <span class="product-price">{{ formatPrice(price, currency) }}</span>
</template>

<style scoped>
.product-price {
  color: var(--color-gold-light);
  font-weight: 600;
}
</style>
