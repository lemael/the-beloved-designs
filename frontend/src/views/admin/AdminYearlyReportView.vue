<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import type { YearlyReport } from '@/types/admin.types'
import { formatPrice } from '@/utils/currency'

const MONTHS = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.']
const FIRST_YEAR = 2026

const currentYear = new Date().getFullYear()
const years = Array.from({ length: Math.max(1, currentYear - FIRST_YEAR + 1) }, (_, i) => currentYear - i)
const year = ref(currentYear)

const report = ref<YearlyReport | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const maxMonthRevenue = computed(() => Math.max(1, ...(report.value?.months.map((m) => Number(m.revenue)) ?? [])))

async function load() {
  loading.value = true
  error.value = null
  try {
    report.value = await adminService.yearlyReport(year.value)
  } catch (e) {
    error.value = apiErrorMessage(e, 'Impossible de charger le bilan.')
  } finally {
    loading.value = false
  }
}

watch(year, load, { immediate: true })
</script>

<template>
  <section>
    <div class="report__header">
      <h1 class="admin-page__title">Bilan de l'année</h1>
      <label class="admin-field report__year">
        <span>Année</span>
        <select v-model.number="year">
          <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
        </select>
      </label>
    </div>

    <p class="admin-muted report__note">
      Commandes payées, expédiées ou livrées uniquement (les commandes en attente ou annulées ne comptent pas).
    </p>

    <p v-if="error" class="admin-error">{{ error }}</p>
    <BaseLoader v-if="loading && !report" />

    <template v-if="report">
      <div class="admin-stats">
        <div class="admin-stat">
          <span class="admin-stat__label">Chiffre d'affaires</span>
          <span class="admin-stat__value">{{ formatPrice(Number(report.revenue), report.currency) }}</span>
        </div>
        <div class="admin-stat">
          <span class="admin-stat__label">Commandes</span>
          <span class="admin-stat__value">{{ report.orderCount }}</span>
        </div>
        <div class="admin-stat">
          <span class="admin-stat__label">Panier moyen</span>
          <span class="admin-stat__value">{{ formatPrice(Math.round(Number(report.averageOrderValue)), report.currency) }}</span>
        </div>
      </div>

      <div class="admin-card">
        <h2 class="admin-card__title">Ventes par mois</h2>
        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Mois</th>
                <th class="num">Commandes</th>
                <th class="num">Chiffre d'affaires</th>
                <th class="report__bar-col"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in report.months" :key="m.month">
                <td>{{ MONTHS[m.month - 1] }}</td>
                <td class="num">{{ m.orderCount }}</td>
                <td class="num">{{ formatPrice(Number(m.revenue), report.currency) }}</td>
                <td class="report__bar-col">
                  <div
                    class="report__bar"
                    :style="{ width: `${(Number(m.revenue) / maxMonthRevenue) * 100}%` }"
                    :aria-hidden="true"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="admin-card">
        <h2 class="admin-card__title">Meilleures ventes</h2>
        <p v-if="report.topProducts.length === 0" class="admin-muted">Aucune vente sur cette année.</p>
        <div v-else class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Produit</th>
                <th class="num">Quantité</th>
                <th class="num">Chiffre d'affaires</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(p, i) in report.topProducts" :key="p.productId">
                <td class="admin-muted">{{ i + 1 }}</td>
                <td>{{ p.productName }}</td>
                <td class="num">{{ p.quantity }}</td>
                <td class="num">{{ formatPrice(Number(p.revenue), report.currency) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="admin-card">
        <h2 class="admin-card__title">Par catégorie</h2>
        <p class="admin-muted report__note">Un produit rangé dans plusieurs catégories compte dans chacune.</p>
        <p v-if="report.byCategory.length === 0" class="admin-muted">Aucune vente sur cette année.</p>
        <div v-else class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Catégorie</th>
                <th class="num">Articles vendus</th>
                <th class="num">Chiffre d'affaires</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in report.byCategory" :key="c.slug">
                <td>{{ c.name }}</td>
                <td class="num">{{ c.quantity }}</td>
                <td class="num">{{ formatPrice(Number(c.revenue), report.currency) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.report__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--spacing-md);
}

.report__year {
  min-width: 120px;
}

.report__note {
  font-size: 0.85rem;
  margin: 0 0 var(--spacing-md);
}

.report__bar-col {
  width: 35%;
}

.report__bar {
  height: 10px;
  min-width: 0;
  background: linear-gradient(90deg, var(--color-gold-dark), var(--color-gold));
  border-radius: var(--radius-sm);
}
</style>
