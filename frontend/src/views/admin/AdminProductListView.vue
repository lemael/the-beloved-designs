<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { DEFAULT_LOW_STOCK_THRESHOLD, findProductList } from '@/features/admin/admin.config'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import type { AdminProduct } from '@/types/admin.types'
import type { Page } from '@/types/product.types'
import { formatPrice } from '@/utils/currency'

const PAGE_SIZE = 20

const route = useRoute()
const list = computed(() => findProductList(route.query.list))
const title = computed(() => (list.value ? list.value.label : 'Tous les produits'))

const page = ref<Page<AdminProduct> | null>(null)
const pageIndex = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    page.value = await adminService.listProducts({
      categories: list.value?.categorySlugs,
      page: pageIndex.value,
      size: PAGE_SIZE,
    })
  } catch (e) {
    error.value = apiErrorMessage(e, 'Impossible de charger les produits.')
  } finally {
    loading.value = false
  }
}

// Changement de liste dans le menu : on revient à la première page
watch(
  () => route.query.list,
  () => {
    pageIndex.value = 0
    page.value = null // pas d'ancien tableau (autre liste) affiché sous le nouveau titre pendant le chargement
    load()
  },
  { immediate: true },
)

function goTo(index: number) {
  pageIndex.value = index
  load()
}

function stockBadge(stock: number) {
  if (stock <= 0) return { label: 'Rupture', cls: 'admin-badge--danger' }
  if (stock <= DEFAULT_LOW_STOCK_THRESHOLD) return { label: 'Stock bas', cls: 'admin-badge--warning' }
  return null
}
</script>

<template>
  <section>
    <div class="product-list__header">
      <h1 class="admin-page__title">{{ title }}</h1>
      <RouterLink
        :to="{ name: 'admin-product-new', query: list ? { list: list.key } : {} }"
        class="product-list__new"
      >
        + Nouveau produit
      </RouterLink>
    </div>

    <p v-if="error" class="admin-error">{{ error }}</p>
    <BaseLoader v-else-if="loading && !page" />

    <div v-else-if="page" class="admin-card">
      <p v-if="page.content.length === 0" class="admin-muted">Aucun produit dans cette liste.</p>

      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th></th>
              <th>Produit</th>
              <th>Catégories</th>
              <th class="num">Prix</th>
              <th class="num">Stock</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in page.content" :key="p.id">
              <td>
                <img v-if="p.imageUrls[0]" :src="p.imageUrls[0]" :alt="p.name" class="product-list__thumb" />
                <div v-else class="product-list__thumb product-list__thumb--empty" />
              </td>
              <td>
                <RouterLink v-if="p.active" :to="`/products/${p.slug}`" target="_blank">{{ p.name }}</RouterLink>
                <span v-else>{{ p.name }}</span>
                <div v-if="p.variants.length" class="admin-muted product-list__variants">
                  {{ p.variants.length }} variante(s)
                </div>
              </td>
              <td class="admin-muted">{{ p.categorySlugs.join(', ') }}</td>
              <td class="num">{{ formatPrice(p.price, p.currency) }}</td>
              <td class="num">
                {{ p.stockQuantity }}
                <span v-if="stockBadge(p.stockQuantity)" class="admin-badge" :class="stockBadge(p.stockQuantity)!.cls">
                  {{ stockBadge(p.stockQuantity)!.label }}
                </span>
              </td>
              <td>
                <span class="admin-badge" :class="p.active ? 'admin-badge--success' : ''">
                  {{ p.active ? 'En ligne' : 'Masqué' }}
                </span>
                <span v-if="p.featured" class="admin-badge admin-badge--warning">Coup de cœur</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="page.totalPages > 1" class="admin-pagination">
        <button class="admin-link-button" :disabled="pageIndex === 0 || loading" @click="goTo(pageIndex - 1)">
          ← Précédent
        </button>
        <span class="admin-muted">Page {{ pageIndex + 1 }} / {{ page.totalPages }} ({{ page.totalElements }} produits)</span>
        <button
          class="admin-link-button"
          :disabled="pageIndex + 1 >= page.totalPages || loading"
          @click="goTo(pageIndex + 1)"
        >
          Suivant →
        </button>
      </div>
      <p v-else class="admin-muted product-list__count">{{ page.totalElements }} produit(s)</p>
    </div>
  </section>
</template>

<style scoped>
.product-list__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--spacing-md);
}

.product-list__new {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-gold);
  border-radius: var(--radius-sm);
}

.product-list__thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  display: block;
}

.product-list__thumb--empty {
  background-color: var(--color-border);
}

.product-list__variants {
  font-size: 0.8rem;
}

.product-list__count {
  margin: var(--spacing-md) 0 0;
  text-align: right;
}

.admin-badge {
  margin-left: var(--spacing-xs);
}
</style>
