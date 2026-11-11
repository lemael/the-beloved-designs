<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import ProductPrice from '@/components/product/ProductPrice.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useCart } from '@/composables/useCart'

const { items, totalPrice, fetchCart } = useCart()

onMounted(fetchCart)
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">Mon panier</h1>
    <p v-if="items.length === 0">Votre panier est vide.</p>
    <ul v-else class="cart-list">
      <li v-for="item in items" :key="item.product.id" class="cart-list__item">
        <span>{{ item.product.name }} × {{ item.quantity }}</span>
        <ProductPrice :price="item.product.price * item.quantity" :currency="item.product.currency" />
      </li>
    </ul>
    <div v-if="items.length > 0" class="cart-total">
      <strong>Total : </strong>
      <ProductPrice :price="totalPrice" currency="FCFA" />
    </div>
    <BaseButton v-if="items.length > 0" @click="$router.push('/checkout')">Passer la commande</BaseButton>
  </AppLayout>
</template>

<style scoped>
.cart-list {
  list-style: none;
  padding: 0;
}

.cart-list__item {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--color-border);
}

.cart-total {
  margin: var(--spacing-lg) 0;
  display: flex;
  gap: var(--spacing-sm);
}
</style>
