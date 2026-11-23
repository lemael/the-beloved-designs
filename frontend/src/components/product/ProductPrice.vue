<script setup lang="ts">
import { formatPrice } from '@/utils/currency'

defineProps<{ price: number; currency: string; promoPrice?: number | null }>()
</script>

<template>
  <span class="product-price">
    <template v-if="promoPrice != null">
      <span class="product-price__promo">{{ formatPrice(promoPrice, currency) }}</span>
      <s class="product-price__old" :aria-label="`Prix normal ${formatPrice(price, currency)}`">
        {{ formatPrice(price, currency) }}
      </s>
    </template>
    <template v-else>{{ formatPrice(price, currency) }}</template>
  </span>
</template>

<style scoped>
.product-price {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--spacing-sm);
  color: var(--color-gold-light);
  font-weight: 600;
}

.product-price__promo {
  color: var(--color-gold);
}

.product-price__old {
  color: var(--color-text-muted);
  font-weight: 400;
  font-size: 0.85em;
}
</style>
