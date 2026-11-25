<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductGrid from '@/components/product/ProductGrid.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import CategoryHero from '@/features/products/components/CategoryHero.vue'
import { CATEGORY_HEROES } from '@/features/products/categoryHeroes'
import { categoryService } from '@/features/products/services/category.service'
import { productService } from '@/features/products/services/product.service'
import type { Category } from '@/types/category.types'
import type { Product } from '@/types/product.types'

const route = useRoute()
const hero = computed(() => CATEGORY_HEROES[String(route.params.slug)])
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
    <CategoryHero v-if="hero" :hero="hero" target-id="produits" />
    <h1 v-else class="font-heading">{{ category?.name }}</h1>
    <div id="produits" class="category-products">
      <BaseLoader v-if="isLoading" />
      <ProductGrid v-else :products="products" />
    </div>
  </AppLayout>
</template>

<style scoped>
.category-products {
  margin-top: var(--spacing-xl);
  scroll-margin-top: var(--spacing-md);
}
</style>
