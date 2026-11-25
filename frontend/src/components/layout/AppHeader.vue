<script setup lang="ts">
import { computed } from 'vue'
import { useCart } from '@/composables/useCart'
import { useFavoritesStore } from '@/stores/favorites.store'

const { totalItems } = useCart()
// Nombre connu du store : chargé quand le client ouvre ses favoris ou en ajoute un.
const favoritesCount = computed(() => useFavoritesStore().items.length)
</script>

<template>
  <header class="app-header">
    <div class="container app-header__inner">
      <RouterLink to="/" class="app-header__logo font-heading text-gold">The Beloved Designs</RouterLink>
      <nav class="app-header__actions" aria-label="Mon espace">
        <RouterLink to="/account" class="app-header__action">
          <svg class="app-header__icon" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" />
          </svg>
          <span class="app-header__label">Mon compte</span>
        </RouterLink>

        <RouterLink to="/favorites" class="app-header__action">
          <span class="app-header__icon-wrap">
            <svg class="app-header__icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 20s-7.5-4.6-9.2-9.3C1.7 7.5 3.8 4.5 7 4.5c2 0 3.4 1.1 5 3 1.6-1.9 3-3 5-3 3.2 0 5.3 3 4.2 6.2C19.5 15.4 12 20 12 20z" />
            </svg>
            <span class="app-header__badge" :aria-label="`${favoritesCount} favori(s)`">{{ favoritesCount }}</span>
          </span>
          <span class="app-header__label">Mes favoris</span>
        </RouterLink>

        <RouterLink to="/cart" class="app-header__action">
          <svg class="app-header__icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M5 8h14l-1 13H6L5 8z" />
            <path d="M9 10V6a3 3 0 0 1 6 0v4" />
          </svg>
          <span class="app-header__label">Panier ({{ totalItems }})</span>
        </RouterLink>
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
  gap: var(--spacing-md);
  padding-block: var(--spacing-md);
}

.app-header__logo {
  font-size: 1.4rem;
}

.app-header__actions {
  display: flex;
  align-items: stretch;
}

.app-header__action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding-inline: var(--spacing-lg);
  color: var(--color-text);
  font-size: 0.85rem;
  white-space: nowrap;
}

/* Séparation verticale or entre les liens, comme sur la maquette. */
.app-header__action + .app-header__action {
  border-left: 1px solid var(--color-gold);
}

.app-header__action:last-child {
  padding-right: 0;
}

.app-header__action:hover,
.app-header__action.router-link-active {
  color: var(--color-gold-light);
}

.app-header__icon-wrap {
  position: relative;
  display: inline-flex;
}

.app-header__icon {
  width: 28px;
  height: 28px;
  fill: none;
  stroke: var(--color-gold);
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.app-header__badge {
  position: absolute;
  top: -6px;
  right: -9px;
  min-width: 17px;
  height: 17px;
  padding-inline: 4px;
  border-radius: 999px;
  background-color: var(--color-gold);
  color: #0b0b0b;
  font-size: 0.68rem;
  font-weight: 700;
  line-height: 17px;
  text-align: center;
}

/* Petit écran : icônes seules (la barre du bas reprend les libellés). */
@media (max-width: 600px) {
  .app-header__action {
    padding-inline: var(--spacing-md);
  }

  .app-header__label {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
    white-space: nowrap;
  }
}
</style>
