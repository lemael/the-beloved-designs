<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductGrid from '@/components/product/ProductGrid.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { productService } from '@/features/products/services/product.service'
import type { Product } from '@/types/product.types'

const products = ref<Product[]>([])
const isLoading = ref(false)

async function load() {
  isLoading.value = true
  const page = await productService.list()
  products.value = page.content
  isLoading.value = false
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">Tous les produits</h1>
    <BaseLoader v-if="isLoading" />
    <ProductGrid v-else :products="products" />
  </AppLayout>
</template>
