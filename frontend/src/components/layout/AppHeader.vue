<script setup lang="ts">
import { useCart } from '@/composables/useCart'

const { totalItems } = useCart()
</script>

<template>
  <header class="app-header">
    <div class="container app-header__inner">
      <RouterLink to="/" class="app-header__logo font-heading text-gold">The Beloved Designs</RouterLink>
      <nav class="app-header__actions">
        <RouterLink to="/account">Mon compte</RouterLink>
        <RouterLink to="/favorites">Mes favoris</RouterLink>
        <RouterLink to="/cart">Panier ({{ totalItems }})</RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-background);
}

.app-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-block: var(--spacing-md);
}

.app-header__logo {
  font-size: 1.4rem;
}

.app-header__actions {
  display: flex;
  gap: var(--spacing-lg);
}
</style>
