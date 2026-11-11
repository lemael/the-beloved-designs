<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductGrid from '@/components/product/ProductGrid.vue'
import { useFavorites } from '@/composables/useFavorites'

const { items, fetchFavorites } = useFavorites()

onMounted(fetchFavorites)
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">Mes favoris</h1>
    <p v-if="items.length === 0">Vous n'avez pas encore de favoris.</p>
    <ProductGrid v-else :products="items" />
  </AppLayout>
</template>
