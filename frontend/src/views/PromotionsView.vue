<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import ProductGrid from '@/components/product/ProductGrid.vue'
import { promotionService } from '@/features/promotions/services/promotion.service'
import type { Promotion } from '@/types/promotion.types'

const promotions = ref<Promotion[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

function endDate(iso: string | null): string | null {
  if (!iso) return null
  return new Date(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', timeZone: 'Africa/Douala' })
}

onMounted(async () => {
  isLoading.value = true
  try {
    promotions.value = await promotionService.list()
  } catch {
    error.value = 'Les promotions ne peuvent pas être chargées pour le moment.'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">Promotions</h1>
    <BaseLoader v-if="isLoading" />
    <p v-else-if="error" class="promotions__message">{{ error }}</p>
    <p v-else-if="promotions.length === 0" class="promotions__message">
      Aucune promotion en ce moment. Revenez bientôt !
    </p>
    <section v-for="promo in promotions" v-else :key="promo.id" class="promotions__block">
      <h2 class="promotions__title">
        -{{ Math.round(promo.discountPercent) }}%
        <span v-if="endDate(promo.endsAt)" class="promotions__until">jusqu'au {{ endDate(promo.endsAt) }}</span>
      </h2>
      <ProductGrid :products="promo.products" />
    </section>
  </AppLayout>
</template>

<style scoped>
.promotions__message {
  color: var(--color-text-muted);
}

.promotions__block {
  margin-bottom: var(--spacing-xl);
}

.promotions__title {
  color: var(--color-danger);
  font-size: 1.2rem;
  margin: var(--spacing-lg) 0 var(--spacing-md);
}

.promotions__until {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 400;
  margin-left: var(--spacing-sm);
}
</style>
