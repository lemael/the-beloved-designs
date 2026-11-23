<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { useAuth } from '@/composables/useAuth'
import { ADMIN_PRODUCT_LISTS, DEFAULT_LOW_STOCK_THRESHOLD } from '@/features/admin/admin.config'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import type { AdminDashboard } from '@/types/admin.types'
import { formatPrice } from '@/utils/currency'

const { user } = useAuth()
const data = ref<AdminDashboard | null>(null)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    data.value = await adminService.dashboard(DEFAULT_LOW_STOCK_THRESHOLD)
  } catch (e) {
    error.value = apiErrorMessage(e, 'Impossible de charger le tableau de bord.')
  }
})
</script>

<template>
  <section>
    <h1 class="admin-page__title">Bonjour {{ user?.firstName }}</h1>

    <p v-if="error" class="admin-error">{{ error }}</p>
    <BaseLoader v-else-if="!data" />

    <template v-else>
      <div class="admin-stats">
        <RouterLink :to="{ name: 'admin-yearly-report' }" class="admin-stat">
          <span class="admin-stat__label">Chiffre d'affaires {{ data.currentYear }}</span>
          <span class="admin-stat__value">{{ formatPrice(data.revenueThisYear, data.currency) }}</span>
        </RouterLink>
        <RouterLink :to="{ name: 'admin-yearly-report' }" class="admin-stat">
          <span class="admin-stat__label">Commandes {{ data.currentYear }}</span>
          <span class="admin-stat__value">{{ data.ordersThisYear }}</span>
        </RouterLink>
        <RouterLink :to="{ name: 'admin-stock' }" class="admin-stat">
          <span class="admin-stat__label">Produits</span>
          <span class="admin-stat__value">{{ data.totalProducts }}</span>
        </RouterLink>
        <RouterLink
          :to="{ name: 'admin-stock' }"
          class="admin-stat"
          :class="{ 'admin-stat--danger': data.outOfStockCount > 0 }"
        >
          <span class="admin-stat__label">En rupture</span>
          <span class="admin-stat__value">{{ data.outOfStockCount }}</span>
        </RouterLink>
        <RouterLink
          :to="{ name: 'admin-stock' }"
          class="admin-stat"
          :class="{ 'admin-stat--warning': data.lowStockCount > 0 }"
        >
          <span class="admin-stat__label">Stock bas (≤ {{ DEFAULT_LOW_STOCK_THRESHOLD }})</span>
          <span class="admin-stat__value">{{ data.lowStockCount }}</span>
        </RouterLink>
        <RouterLink :to="{ name: 'admin-restocks' }" class="admin-stat">
          <span class="admin-stat__label">Ravitaillements prévus</span>
          <span class="admin-stat__value">{{ data.pendingRestocks }}</span>
        </RouterLink>
      </div>

      <div class="admin-card">
        <h2 class="admin-card__title">Accès rapide</h2>
        <div class="admin-actions">
          <RouterLink :to="{ name: 'admin-product-new' }">+ Créer un produit</RouterLink>
          <RouterLink
            v-for="list in ADMIN_PRODUCT_LISTS"
            :key="list.key"
            :to="{ name: 'admin-products', query: { list: list.key } }"
          >
            {{ list.label }}
          </RouterLink>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
a.admin-stat {
  color: inherit;
  transition: border-color 0.2s ease;
}

a.admin-stat:hover {
  border-color: var(--color-gold);
}

.admin-actions a {
  padding: var(--spacing-xs) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
</style>
