<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductPrice from '@/components/product/ProductPrice.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { productService } from '@/features/products/services/product.service'
import { useCart } from '@/composables/useCart'
import type { Product } from '@/types/product.types'

const route = useRoute()
const product = ref<Product | null>(null)
const isLoading = ref(false)
const { addItem } = useCart()

async function load() {
  isLoading.value = true
  product.value = await productService.getBySlug(String(route.params.slug))
  isLoading.value = false
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <AppLayout>
    <BaseLoader v-if="isLoading || !product" />
    <article v-else class="product-detail">
      <div class="product-detail__image">
        <img v-if="product.imageUrls[0]" :src="product.imageUrls[0]" :alt="product.name" />
      </div>
      <div class="product-detail__info">
        <h1 class="font-heading">{{ product.name }}</h1>
        <ProductPrice :price="product.price" :currency="product.currency" />
        <p>{{ product.description }}</p>
        <BaseButton @click="addItem(product.id)">Ajouter au panier</BaseButton>
      </div>
    </article>
  </AppLayout>
</template>

<style scoped>
.product-detail {
  display: grid;
  gap: var(--spacing-lg);
}

@media (min-width: 768px) {
  .product-detail {
    grid-template-columns: 1fr 1fr;
  }
}

.product-detail__image img {
  width: 100%;
  border-radius: var(--radius-md);
}
</style>
