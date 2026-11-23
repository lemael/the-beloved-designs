<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { DEFAULT_LOW_STOCK_THRESHOLD } from '@/features/admin/admin.config'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import type { StockSummary } from '@/types/admin.types'

const threshold = ref<number>(DEFAULT_LOW_STOCK_THRESHOLD)
const summary = ref<StockSummary | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  if (!Number.isInteger(threshold.value) || threshold.value < 0 || threshold.value > 1000) {
    error.value = 'Le seuil doit être un nombre entier entre 0 et 1000.'
    return
  }
  loading.value = true
  error.value = null
  try {
    summary.value = await adminService.stockSummary(threshold.value)
  } catch (e) {
    error.value = apiErrorMessage(e, 'Impossible de charger le bilan de stock.')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="admin-page__title">Bilan actuel du stock</h1>

    <form class="admin-card admin-actions" @submit.prevent="load">
      <label class="admin-field stock__threshold">
        <span>Seuil « stock bas »</span>
        <input v-model.number="threshold" type="number" min="0" max="1000" step="1" />
      </label>
      <BaseButton type="submit" variant="ghost" :disabled="loading">Actualiser</BaseButton>
    </form>

    <p v-if="error" class="admin-error">{{ error }}</p>
    <BaseLoader v-if="loading && !summary" />

    <template v-if="summary">
      <div class="admin-stats">
        <div class="admin-stat">
          <span class="admin-stat__label">Produits</span>
          <span class="admin-stat__value">{{ summary.totalProducts }}</span>
        </div>
        <div class="admin-stat">
          <span class="admin-stat__label">Unités en stock</span>
          <span class="admin-stat__value">{{ summary.totalUnits }}</span>
        </div>
        <div class="admin-stat" :class="{ 'admin-stat--danger': summary.outOfStockCount > 0 }">
          <span class="admin-stat__label">En rupture</span>
          <span class="admin-stat__value">{{ summary.outOfStockCount }}</span>
        </div>
        <div class="admin-stat" :class="{ 'admin-stat--warning': summary.lowStockCount > 0 }">
          <span class="admin-stat__label">Stock bas (1 à {{ summary.lowStockThreshold }})</span>
          <span class="admin-stat__value">{{ summary.lowStockCount }}</span>
        </div>
      </div>

      <div class="admin-card">
        <h2 class="admin-card__title">À réapprovisionner</h2>
        <p v-if="summary.lowStockProducts.length === 0" class="admin-muted">
          Aucun produit en rupture ou sous le seuil. 👍
        </p>
        <div v-else class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Produit</th>
                <th class="num">Stock</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in summary.lowStockProducts" :key="p.id">
                <td>
                  {{ p.name }}
                  <span v-if="!p.active" class="admin-badge">Masqué</span>
                </td>
                <td class="num">{{ p.stockQuantity }}</td>
                <td>
                  <span class="admin-badge" :class="p.stockQuantity <= 0 ? 'admin-badge--danger' : 'admin-badge--warning'">
                    {{ p.stockQuantity <= 0 ? 'Rupture' : 'Stock bas' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <RouterLink :to="{ name: 'admin-restocks' }" class="stock__restock-link">
          Planifier un ravitaillement →
        </RouterLink>
      </div>

      <div class="admin-card">
        <h2 class="admin-card__title">Par catégorie</h2>
        <p class="admin-muted stock__note">Un produit rangé dans plusieurs catégories apparaît dans chacune.</p>
        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Catégorie</th>
                <th class="num">Produits</th>
                <th class="num">Unités</th>
                <th class="num">En rupture</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in summary.byCategory" :key="c.slug">
                <td>{{ c.name }}</td>
                <td class="num">{{ c.productCount }}</td>
                <td class="num">{{ c.units }}</td>
                <td class="num" :class="{ 'stock__danger': c.outOfStockCount > 0 }">{{ c.outOfStockCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.stock__threshold {
  max-width: 200px;
}

.admin-actions {
  align-items: end;
}

.stock__restock-link {
  display: inline-block;
  margin-top: var(--spacing-md);
}

.stock__note {
  margin-top: calc(-1 * var(--spacing-sm));
  font-size: 0.85rem;
}

.stock__danger {
  color: var(--color-danger);
  font-weight: 600;
}
</style>
