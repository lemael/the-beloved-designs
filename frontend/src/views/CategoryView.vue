<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductGrid from '@/components/product/ProductGrid.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { categoryService } from '@/features/products/services/category.service'
import { productService } from '@/features/products/services/product.service'
import type { Category } from '@/types/category.types'
import type { Product } from '@/types/product.types'

const route = useRoute()
const category = ref<Category | null>(null)
const products = ref<Product[]>([])
const isLoading = ref(false)

async function load() {
  const slug = String(route.params.slug)
  isLoading.value = true
  const [categoryResult, productsPage] = await Promise.all([
    categoryService.getBySlug(slug),
    productService.list({ category: slug }),
  ])
  category.value = categoryResult
  products.value = productsPage.content
  isLoading.value = false
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">{{ category?.name }}</h1>
    <BaseLoader v-if="isLoading" />
    <ProductGrid v-else :products="products" />
  </AppLayout>
</template>
