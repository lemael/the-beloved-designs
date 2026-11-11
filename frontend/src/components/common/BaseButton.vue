<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'ghost'
    type?: 'button' | 'submit'
  }>(),
  { variant: 'primary', type: 'button' },
)
</script>

<template>
  <button :type="type" class="base-button" :class="variant">
    <slot />
  </button>
</template>

<style scoped>
.base-button {
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-sm);
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-gold);
  transition: all 0.2s ease;
}

.base-button.primary {
  background-color: var(--color-gold);
  color: #0b0b0b;
}

.base-button.primary:hover {
  background-color: var(--color-gold-light);
}

.base-button.ghost {
  background-color: transparent;
  color: var(--color-gold);
}

.base-button.ghost:hover {
  background-color: var(--color-surface);
}
</style>
