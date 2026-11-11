<script setup lang="ts">
const categories = [
  { label: 'Femmes', slug: 'femmes' },
  { label: 'Hommes', slug: 'hommes' },
  { label: 'Chaussures', slug: 'chaussures' },
  { label: 'Sacs à main', slug: 'sacs-a-main' },
  { label: 'Cosmétiques', slug: 'cosmetiques' },
]
</script>

<template>
  <nav class="app-navbar">
    <div class="container app-navbar__inner">
      <RouterLink v-for="c in categories" :key="c.slug" :to="`/categories/${c.slug}`">{{ c.label }}</RouterLink>
      <RouterLink to="/promotions" class="app-navbar__promo">Promotions</RouterLink>
    </div>
  </nav>
</template>

<style scoped>
.app-navbar {
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.app-navbar__inner {
  display: flex;
  gap: var(--spacing-lg);
  padding-block: var(--spacing-sm);
  overflow-x: auto;
}

.app-navbar__promo {
  color: var(--color-danger);
  font-weight: 600;
}
</style>
