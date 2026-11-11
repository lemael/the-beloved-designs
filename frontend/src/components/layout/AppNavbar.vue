<script setup lang="ts">
import { ref } from 'vue'

const categories = [
  { label: 'Femmes', slug: 'femmes' },
  { label: 'Hommes', slug: 'hommes' },
  { label: 'Chaussures', slug: 'chaussures' },
  { label: 'Sacs à main', slug: 'sacs-a-main' },
  { label: 'Cosmétiques', slug: 'cosmetiques' },
]

const isOpen = ref(false)

function close() {
  isOpen.value = false
}
</script>

<template>
  <nav class="app-navbar">
    <div class="container app-navbar__bar">
      <button
        class="app-navbar__toggle"
        type="button"
        :aria-expanded="isOpen"
        aria-controls="app-navbar-menu"
        aria-label="Ouvrir les catégories"
        @click="isOpen = !isOpen"
      >
        <span />
        <span />
        <span />
      </button>

      <div id="app-navbar-menu" class="app-navbar__inner" :class="{ 'is-open': isOpen }">
        <RouterLink
          v-for="c in categories"
          :key="c.slug"
          :to="`/categories/${c.slug}`"
          @click="close"
        >
          {{ c.label }}
        </RouterLink>
        <RouterLink to="/promotions" class="app-navbar__promo" @click="close">Promotions</RouterLink>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.app-navbar {
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.app-navbar__bar {
  display: flex;
  align-items: center;
  padding-block: var(--spacing-sm);
}

/* Hamburger : caché sur desktop, visible sur mobile */
.app-navbar__toggle {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 32px;
  height: 32px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.app-navbar__toggle span {
  display: block;
  height: 2px;
  background-color: var(--color-gold);
  border-radius: 1px;
}

.app-navbar__inner {
  display: flex;
  gap: var(--spacing-lg);
  overflow-x: auto;
}

.app-navbar__promo {
  color: var(--color-danger);
  font-weight: 600;
}

/* Mobile : catégories repliées sous le hamburger */
@media (max-width: 767px) {
  .app-navbar__inner {
    display: none;
    flex-direction: column;
    gap: 0;
    width: 100%;
  }

  .app-navbar__inner.is-open {
    display: flex;
  }

  .app-navbar__inner a {
    padding: var(--spacing-sm) 0;
    border-bottom: 1px solid var(--color-border);
  }
}

/* Desktop : hamburger masqué, catégories toujours visibles en ligne */
@media (min-width: 768px) {
  .app-navbar__toggle {
    display: none;
  }

  .app-navbar__inner {
    display: flex !important;
  }
}
</style>
