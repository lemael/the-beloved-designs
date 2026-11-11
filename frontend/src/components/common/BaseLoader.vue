<template>
  <div class="base-loader" role="status" aria-label="Chargement">
    <span class="base-loader__spinner" />
  </div>
</template>

<style scoped>
.base-loader {
  display: flex;
  justify-content: center;
  padding: var(--spacing-lg);
}

.base-loader__spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-gold);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
