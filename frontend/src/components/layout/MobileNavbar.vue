<script setup lang="ts">
import { useCart } from '@/composables/useCart'

const { totalItems } = useCart()
</script>

<template>
  <nav class="mobile-navbar">
    <RouterLink to="/">Accueil</RouterLink>
    <RouterLink to="/favorites">Favoris</RouterLink>
    <RouterLink to="/cart">Panier ({{ totalItems }})</RouterLink>
    <RouterLink to="/account">Compte</RouterLink>
  </nav>
</template>

<style scoped>
.mobile-navbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  background-color: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: var(--spacing-sm);
}

@media (min-width: 768px) {
  .mobile-navbar {
    display: none;
  }
}
</style>
