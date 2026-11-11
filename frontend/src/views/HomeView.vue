<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductCarousel from '@/components/product/ProductCarousel.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { useProducts } from '@/composables/useProducts'

const { products, isLoading, error, fetchFeatured } = useProducts()

onMounted(fetchFeatured)
</script>

<template>
  <AppLayout>
    <section class="hero">
      <h1 class="font-heading text-gold">The Beloved Designs</h1>
      <p class="tagline">Affirmez votre style, en toute confiance !</p>
    </section>

    <section class="favorites">
      <h2>Nos Produits Coup de Cœur</h2>
      <BaseLoader v-if="isLoading" />
      <p v-else-if="error">{{ error }}</p>
      <ProductCarousel v-else :products="products" />
    </section>
  </AppLayout>
</template>

<style scoped>
.hero {
  text-align: center;
  padding-block: var(--spacing-xl);
}

.favorites {
  margin-top: var(--spacing-xl);
}
</style>
