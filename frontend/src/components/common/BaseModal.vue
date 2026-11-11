<script setup lang="ts">
defineProps<{ modelValue: boolean; title?: string }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<template>
  <div v-if="modelValue" class="base-modal__overlay" @click.self="$emit('update:modelValue', false)">
    <div class="base-modal">
      <header v-if="title" class="base-modal__header">
        <h3>{{ title }}</h3>
        <button class="base-modal__close" @click="$emit('update:modelValue', false)">×</button>
      </header>
      <div class="base-modal__body">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.base-modal__overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.base-modal {
  background-color: var(--color-surface);
  border: 1px solid var(--color-gold);
  border-radius: var(--radius-md);
  min-width: 320px;
  max-width: 90vw;
  padding: var(--spacing-lg);
}

.base-modal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.base-modal__close {
  background: none;
  border: none;
  color: var(--color-text);
  font-size: 1.5rem;
  cursor: pointer;
}
</style>
