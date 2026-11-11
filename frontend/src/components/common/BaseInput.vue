<script setup lang="ts">
defineProps<{
  modelValue: string
  label?: string
  type?: string
  error?: string
}>()

defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <label class="base-input">
    <span v-if="label" class="base-input__label">{{ label }}</span>
    <input
      :type="type ?? 'text'"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <span v-if="error" class="base-input__error">{{ error }}</span>
  </label>
</template>

<style scoped>
.base-input {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.base-input__label {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

input {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  color: var(--color-text);
}

input:focus {
  outline: none;
  border-color: var(--color-gold);
}

.base-input__error {
  color: var(--color-danger);
  font-size: 0.8rem;
}
</style>
