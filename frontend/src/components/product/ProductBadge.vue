<script setup lang="ts">
import BaseBadge from '@/components/common/BaseBadge.vue'

defineProps<{ featured?: boolean; discountPercent?: number | null }>()
</script>

<template>
  <span class="product-badges">
    <BaseBadge v-if="discountPercent" variant="discount">-{{ Math.round(discountPercent) }}%</BaseBadge>
    <BaseBadge v-if="featured" variant="new">Nouveau</BaseBadge>
  </span>
</template>

<style scoped>
.product-badges {
  display: inline-flex;
  gap: var(--spacing-xs);
}
</style>
