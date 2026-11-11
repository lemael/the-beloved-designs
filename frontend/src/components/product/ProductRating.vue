<script setup lang="ts">
withDefaults(defineProps<{ rating?: number }>(), { rating: 0 })
</script>

<template>
  <div class="product-rating" :aria-label="`${rating} sur 5`">
    <span v-for="n in 5" :key="n" :class="{ filled: n <= rating }">★</span>
  </div>
</template>

<style scoped>
.product-rating span {
  color: var(--color-border);
}

.product-rating span.filled {
  color: var(--color-gold);
}
</style>
