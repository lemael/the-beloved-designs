<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'new' | 'discount' }>(), { variant: 'new' })
</script>

<template>
  <span class="base-badge" :class="variant">
    <slot />
  </span>
</template>

<style scoped>
.base-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.base-badge.new {
  background-color: var(--color-gold);
  color: #0b0b0b;
}

.base-badge.discount {
  background-color: var(--color-danger);
  color: #fff;
}
</style>
