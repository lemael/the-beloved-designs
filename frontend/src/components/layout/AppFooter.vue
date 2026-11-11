<template>
  <footer class="app-footer">
    <div class="container app-footer__inner">
      <p class="font-heading text-gold">The Beloved Designs</p>
      <p>Service client : +237 6 12 34 56 78</p>
      <p>&copy; {{ new Date().getFullYear() }} The Beloved Designs — Mode • Beauté • Élégance</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  border-top: 1px solid var(--color-border);
  margin-top: var(--spacing-xl);
}

.app-footer__inner {
  padding-block: var(--spacing-lg);
  text-align: center;
  color: var(--color-text-muted);
}
</style>
