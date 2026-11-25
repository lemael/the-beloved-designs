<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductCarousel from '@/components/product/ProductCarousel.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import CategoryTiles from '@/features/products/components/CategoryTiles.vue'
import { useProducts } from '@/composables/useProducts'

const { products, isLoading, error, fetchFeatured } = useProducts()
const carousel = ref<InstanceType<typeof ProductCarousel> | null>(null)

onMounted(fetchFeatured)
</script>

<template>
  <AppLayout>
    <section class="hero">
      <svg class="hero__crown" viewBox="0 0 64 40" aria-hidden="true">
        <defs>
          <linearGradient id="hero-gold" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#f6e3a1" />
            <stop offset="0.5" stop-color="#d4a94a" />
            <stop offset="1" stop-color="#9a7428" />
          </linearGradient>
        </defs>
        <path
          fill="url(#hero-gold)"
          d="M6 14 L19 24 L32 4 L45 24 L58 14 L53 32 H11 Z M11 34 H53 V38 H11 Z"
        />
        <circle cx="6" cy="12" r="3" fill="url(#hero-gold)" />
        <circle cx="32" cy="3" r="3" fill="url(#hero-gold)" />
        <circle cx="58" cy="12" r="3" fill="url(#hero-gold)" />
      </svg>
      <h1 class="hero__title">The Beloved Designs</h1>
      <p class="hero__subtitle"><span>Mode &amp; Élégance pour Elle et Lui</span></p>
      <span class="hero__rule" aria-hidden="true"></span>
      <p class="hero__tagline">Affirmez votre style,<br />en toute confiance !</p>
      <svg class="hero__swoosh" viewBox="0 0 200 14" aria-hidden="true">
        <path d="M4 12 Q100 -2 196 8" fill="none" stroke="url(#hero-gold)" stroke-width="3" stroke-linecap="round" />
      </svg>
    </section>

    <section class="categories">
      <CategoryTiles />
    </section>

    <section class="favorites">
      <header class="favorites__header">
        <h2 class="favorites__title font-heading text-gold">
          <span aria-hidden="true">♛</span> Nos Produits Coup de Cœur
        </h2>
        <div v-if="products.length" class="favorites__arrows">
          <button type="button" aria-label="Produits précédents" @click="carousel?.scrollPage(-1)">‹</button>
          <button type="button" aria-label="Produits suivants" @click="carousel?.scrollPage(1)">›</button>
        </div>
      </header>
      <BaseLoader v-if="isLoading" />
      <p v-else-if="error">{{ error }}</p>
      <ProductCarousel v-else ref="carousel" :products="products" />
    </section>
  </AppLayout>
</template>

<style scoped>
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-block: var(--spacing-xl);
}

.hero__crown {
  width: 64px;
  height: 40px;
  filter: drop-shadow(0 2px 6px rgba(212, 169, 74, 0.45));
}

/* Or métallique : dégradé clair → foncé appliqué au texte. */
.hero__title {
  margin: var(--spacing-xs, 4px) 0 0;
  font-family: var(--font-heading);
  font-style: italic;
  font-weight: 700;
  font-size: clamp(1.6rem, 8vw, 4rem);
  line-height: 1.1;
  letter-spacing: 0.01em;
  background: linear-gradient(180deg, #fbeab0 0%, #e2b955 45%, #b8862f 70%, #f1d27c 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 3px 4px rgba(0, 0, 0, 0.6));
}

.hero__subtitle {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin: var(--spacing-xs, 4px) 0 0;
  font-family: var(--font-heading);
  font-weight: 500;
  font-size: clamp(1rem, 2.4vw, 1.5rem);
  color: var(--color-gold-light);
}

/* typography.css impose la police du texte aux <span> : on la remet ici. */
.hero__subtitle span {
  font-family: inherit;
}

.hero__subtitle::before,
.hero__subtitle::after {
  content: '';
  width: clamp(24px, 6vw, 64px);
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-gold));
}

.hero__subtitle::after {
  background: linear-gradient(90deg, var(--color-gold), transparent);
}

.hero__rule {
  width: min(70%, 520px);
  height: 2px;
  margin-block: var(--spacing-md);
  background: linear-gradient(90deg, transparent, var(--color-gold-light), transparent);
}

.hero__tagline {
  margin: 0;
  font-family: var(--font-heading);
  font-style: italic;
  font-size: clamp(1.3rem, 3.4vw, 2.1rem);
  line-height: 1.25;
  color: #fff;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.7);
}

.hero__swoosh {
  width: min(45%, 220px);
  height: 14px;
  margin-top: var(--spacing-xs, 4px);
}

.categories {
  margin-top: var(--spacing-lg);
}

.favorites {
  margin-top: var(--spacing-xl);
}

.favorites__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-gold);
}

.favorites__title {
  flex: 1;
  margin: 0;
}

.favorites__arrows {
  display: flex;
  gap: var(--spacing-sm);
}

.favorites__arrows button {
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-gold);
  border-radius: 50%;
  background: transparent;
  color: var(--color-gold);
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
}

.favorites__arrows button:hover {
  background-color: var(--color-gold);
  color: #0b0b0b;
}
</style>
