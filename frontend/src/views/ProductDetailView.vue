<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductPrice from '@/components/product/ProductPrice.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { productService } from '@/features/products/services/product.service'
import { useAddToCart } from '@/features/cart/composables/useAddToCart'
import type { Product } from '@/types/product.types'

const route = useRoute()
const product = ref<Product | null>(null)
const isLoading = ref(false)
const { quantity, status, error, inStock, maxQuantity, add } = useAddToCart(product)

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
        <ProductPrice :price="product.price" :promo-price="product.promoPrice" :currency="product.currency" />
        <p>{{ product.description }}</p>

        <p v-if="!inStock" class="product-detail__stock product-detail__stock--out">Rupture de stock</p>

        <form v-else class="add-to-cart" @submit.prevent="add">
          <label class="add-to-cart__quantity">
            <span>Quantité</span>
            <select v-model.number="quantity" :disabled="status === 'adding'">
              <option v-for="n in maxQuantity" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
          <BaseButton type="submit" :disabled="status === 'adding'" :aria-busy="status === 'adding'">
            {{ status === 'adding' ? 'Ajout…' : 'Ajouter au panier' }}
          </BaseButton>
        </form>

        <!-- Annonce aussi le résultat aux lecteurs d'écran -->
        <div class="add-to-cart__feedback" aria-live="polite">
          <p v-if="status === 'added'" class="add-to-cart__success">
            ✓ Ajouté au panier.
            <RouterLink to="/cart">Voir le panier</RouterLink>
          </p>
          <p v-else-if="status === 'error'" class="add-to-cart__error" role="alert">{{ error }}</p>
        </div>
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

.product-detail__stock--out {
  /* Rouge éclairci : --color-danger (#c0392b) est trop peu contrasté sur fond noir pour du texte. */
  color: #ff6b5e;
  font-weight: 600;
}

.add-to-cart {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  margin-top: var(--spacing-md);
}

.add-to-cart__quantity {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.add-to-cart__quantity select {
  min-width: 72px;
  padding: var(--spacing-sm);
  border: 1px solid var(--color-gold-dark);
  border-radius: var(--radius-sm);
  background-color: var(--color-surface);
  color: var(--color-text);
  font-size: 1rem;
}

.add-to-cart :deep(.base-button:disabled) {
  opacity: 0.6;
  cursor: wait;
}

.add-to-cart__feedback {
  min-height: 1.5em;
  margin-top: var(--spacing-sm);
}

.add-to-cart__success {
  color: var(--color-gold-light);
}

.add-to-cart__success a {
  margin-left: var(--spacing-sm);
  color: var(--color-gold);
  text-decoration: underline;
}

.add-to-cart__error {
  color: #ff6b5e;
}
</style>
