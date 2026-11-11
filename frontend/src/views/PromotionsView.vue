<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { promotionService } from '@/features/promotions/services/promotion.service'
import type { Promotion } from '@/types/promotion.types'

const promotions = ref<Promotion[]>([])
const isLoading = ref(false)

onMounted(async () => {
  isLoading.value = true
  promotions.value = await promotionService.list()
  isLoading.value = false
})
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">Promotions</h1>
    <BaseLoader v-if="isLoading" />
    <ul v-else>
      <li v-for="promo in promotions" :key="promo.id">
        {{ promo.label }} — -{{ promo.discountPercent }}%
      </li>
    </ul>
  </AppLayout>
</template>
