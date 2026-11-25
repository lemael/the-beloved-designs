<script setup lang="ts">
import { reactive } from 'vue'
import { CATEGORY_TILES } from '@/features/products/categoryTiles'

// Une photo manquante ne casse pas la tuile : visuel de secours doré à la place.
const missing = reactive<Record<string, boolean>>({})
</script>

<template>
  <nav class="category-tiles" aria-label="Catégories">
    <RouterLink v-for="tile in CATEGORY_TILES" :key="tile.to" :to="tile.to" class="category-tile">
      <span class="category-tile__media">
        <img
          v-if="!missing[tile.image]"
          :src="`/images/categories/${tile.image}.jpg`"
          :alt="tile.label"
          loading="lazy"
          @error="missing[tile.image] = true"
        />
        <span v-else class="category-tile__fallback" aria-hidden="true">{{ tile.label.charAt(0) }}</span>
      </span>
      <span class="category-tile__label">{{ tile.label }}</span>
      <span class="category-tile__cta">Voir tout →</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.category-tiles {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
}

@media (min-width: 768px) {
  .category-tiles {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .category-tiles {
    grid-template-columns: repeat(6, 1fr);
  }
}

.category-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background-color: var(--color-surface);
  border: 1px solid var(--color-gold);
  border-radius: var(--radius-lg);
  color: var(--color-text);
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.category-tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 12px rgba(201, 162, 75, 0.35);
}

@media (prefers-reduced-motion: reduce) {
  .category-tile {
    transition: none;
  }

  .category-tile:hover {
    transform: none;
  }
}

.category-tile__media {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 2 / 1;
  border-radius: var(--radius-md);
  background-color: #000;
  overflow: hidden;
}

.category-tile__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.category-tile__fallback {
  font-family: var(--font-heading);
  font-size: 3rem;
  color: var(--color-gold);
}

.category-tile__label {
  font-weight: 600;
  font-size: 0.95rem;
}

.category-tile__cta {
  padding: 2px var(--spacing-md);
  border-radius: 999px;
  background-color: var(--color-gold);
  color: #0b0b0b;
  font-size: 0.75rem;
  font-weight: 700;
}
</style>
