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
      <h1 class="font-heading text-gold">The Beloved Designs</h1>
      <p class="tagline">Affirmez votre style, en toute confiance !</p>
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
  text-align: center;
  padding-block: var(--spacing-xl);
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
