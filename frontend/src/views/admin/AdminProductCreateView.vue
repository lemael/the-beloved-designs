<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BaseLoader from '@/components/common/BaseLoader.vue'
import ProductForm from '@/features/admin/components/ProductForm.vue'
import { findProductList } from '@/features/admin/admin.config'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import { categoryService } from '@/features/products/services/category.service'
import type { AdminProduct, CreateProductPayload } from '@/types/admin.types'
import type { Category } from '@/types/category.types'

const route = useRoute()
// Ouvert depuis une liste (ex: chaussures) → catégorie pré-cochée, sauf "habits" (femmes ou hommes ?)
const fromList = computed(() => findProductList(route.query.list))
const initialCategorySlugs = computed(() =>
  fromList.value && fromList.value.categorySlugs.length === 1 ? fromList.value.categorySlugs : [],
)

const categories = ref<Category[] | null>(null)
const loadError = ref<string | null>(null)
const submitting = ref(false)
const serverError = ref<string | null>(null)
const created = ref<AdminProduct | null>(null)
const formKey = ref(0)

onMounted(async () => {
  try {
    categories.value = await categoryService.list()
  } catch (e) {
    loadError.value = apiErrorMessage(e, 'Impossible de charger les catégories.')
  }
})

async function onSubmit(payload: CreateProductPayload) {
  submitting.value = true
  serverError.value = null
  try {
    created.value = await adminService.createProduct(payload)
    formKey.value++ // formulaire vide pour enchaîner une autre création
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    serverError.value = apiErrorMessage(e, "Le produit n'a pas pu être créé.")
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="product-create">
    <h1 class="admin-page__title">Créer un nouveau produit</h1>

    <div v-if="created" class="admin-card product-create__done" role="status">
      <p class="admin-success">
        « {{ created.name }} » a été créé
        {{ created.active ? 'et est visible dans la boutique' : '(masqué dans la boutique)' }}.
      </p>
      <div class="admin-actions">
        <RouterLink v-if="created.active" :to="`/products/${created.slug}`" target="_blank">Voir la fiche</RouterLink>
        <RouterLink v-if="fromList" :to="{ name: 'admin-products', query: { list: fromList.key } }">
          Retour à la liste {{ fromList.label.toLowerCase() }}
        </RouterLink>
      </div>
    </div>

    <p v-if="loadError" class="admin-error">{{ loadError }}</p>
    <BaseLoader v-else-if="!categories" />
    <ProductForm
      v-else
      :key="formKey"
      :categories="categories"
      :submitting="submitting"
      :server-error="serverError"
      :initial-category-slugs="initialCategorySlugs"
      @submit="onSubmit"
    />
  </section>
</template>

<style scoped>
.product-create {
  max-width: 820px;
}

.product-create__done {
  border-color: var(--color-success);
}

.product-create__done .admin-actions {
  margin-top: var(--spacing-sm);
}
</style>
