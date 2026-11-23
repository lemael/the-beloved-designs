<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import BaseButton from '@/components/common/BaseButton.vue'
import type { Category } from '@/types/category.types'
import type { CreateProductPayload } from '@/types/admin.types'

const props = defineProps<{
  categories: Category[]
  submitting?: boolean
  serverError?: string | null
  /** Catégories cochées au départ (ex: ouvert depuis "Liste des chaussures"). */
  initialCategorySlugs?: string[]
}>()

const emit = defineEmits<{ submit: [payload: CreateProductPayload] }>()

interface VariantRow {
  size: string
  color: string
  stockQuantity: number | null
}

const form = reactive({
  name: '',
  slug: '',
  description: '',
  price: null as number | null,
  stockQuantity: 0 as number | null,
  featured: false,
  active: true,
  categorySlugs: [...(props.initialCategorySlugs ?? [])] as string[],
  imageUrlsText: '',
  variants: [] as VariantRow[],
})

const localError = ref<string | null>(null)
const error = computed(() => localError.value ?? props.serverError ?? null)

function addVariant() {
  form.variants.push({ size: '', color: '', stockQuantity: 0 })
}

function removeVariant(index: number) {
  form.variants.splice(index, 1)
}

function onSubmit() {
  localError.value = null
  if (!form.name.trim()) {
    localError.value = 'Le nom du produit est obligatoire.'
    return
  }
  if (form.price === null || !(form.price > 0)) {
    localError.value = 'Le prix doit être supérieur à 0.'
    return
  }
  if (form.categorySlugs.length === 0) {
    localError.value = 'Choisissez au moins une catégorie.'
    return
  }

  emit('submit', {
    name: form.name.trim(),
    slug: form.slug.trim() || undefined,
    description: form.description.trim() || undefined,
    price: form.price,
    stockQuantity: form.stockQuantity ?? 0,
    featured: form.featured,
    active: form.active,
    categorySlugs: form.categorySlugs,
    imageUrls: form.imageUrlsText
      .split('\n')
      .map((url) => url.trim())
      .filter(Boolean),
    variants: form.variants
      .filter((v) => v.size.trim() || v.color.trim())
      .map((v) => ({
        size: v.size.trim() || undefined,
        color: v.color.trim() || undefined,
        stockQuantity: v.stockQuantity ?? 0,
      })),
  })
}
</script>

<template>
  <form class="admin-form" novalidate @submit.prevent="onSubmit">
    <section class="admin-card">
      <h2 class="admin-card__title">Informations</h2>
      <div class="admin-form">
        <label class="admin-field">
          <span>Nom du produit *</span>
          <input v-model="form.name" type="text" maxlength="255" required />
        </label>
        <label class="admin-field">
          <span>Slug (adresse du produit) — laissé vide, il est généré à partir du nom</span>
          <input v-model="form.slug" type="text" maxlength="255" placeholder="ex: sac-cuir-noir" />
        </label>
        <label class="admin-field">
          <span>Description</span>
          <textarea v-model="form.description" rows="4" />
        </label>
        <div class="admin-form__row">
          <label class="admin-field">
            <span>Prix (FCFA) *</span>
            <input v-model.number="form.price" type="number" min="1" step="1" required />
          </label>
          <label class="admin-field">
            <span>Stock total</span>
            <input v-model.number="form.stockQuantity" type="number" min="0" step="1" />
          </label>
        </div>
        <div class="admin-actions">
          <label class="admin-check">
            <input v-model="form.active" type="checkbox" />
            Visible dans la boutique
          </label>
          <label class="admin-check">
            <input v-model="form.featured" type="checkbox" />
            Coup de cœur (mis en avant sur l'accueil)
          </label>
        </div>
      </div>
    </section>

    <section class="admin-card">
      <h2 class="admin-card__title">Catégories *</h2>
      <p v-if="categories.length === 0" class="admin-muted">
        Aucune catégorie en base : créez-les avant d'ajouter un produit.
      </p>
      <div class="admin-actions">
        <label v-for="c in categories" :key="c.slug" class="admin-check">
          <input v-model="form.categorySlugs" type="checkbox" :value="c.slug" />
          {{ c.name }}
        </label>
      </div>
    </section>

    <section class="admin-card">
      <h2 class="admin-card__title">Images</h2>
      <label class="admin-field">
        <span>Adresses des images (une par ligne, la première est l'image principale)</span>
        <textarea v-model="form.imageUrlsText" rows="3" placeholder="https://..." />
      </label>
    </section>

    <section class="admin-card">
      <h2 class="admin-card__title">Variantes (tailles / couleurs)</h2>
      <p class="admin-muted">Optionnel. Le stock total ci-dessus reste la référence pour le bilan de stock.</p>
      <div v-for="(v, i) in form.variants" :key="i" class="admin-form__row product-form__variant">
        <label class="admin-field">
          <span>Taille</span>
          <input v-model="v.size" type="text" maxlength="50" placeholder="ex: M, 38" />
        </label>
        <label class="admin-field">
          <span>Couleur</span>
          <input v-model="v.color" type="text" maxlength="50" placeholder="ex: Noir" />
        </label>
        <label class="admin-field">
          <span>Stock</span>
          <input v-model.number="v.stockQuantity" type="number" min="0" step="1" />
        </label>
        <button type="button" class="admin-link-button product-form__remove" @click="removeVariant(i)">
          Retirer
        </button>
      </div>
      <BaseButton variant="ghost" @click="addVariant">+ Ajouter une variante</BaseButton>
    </section>

    <p v-if="error" class="admin-error" role="alert">{{ error }}</p>
    <div class="admin-actions">
      <BaseButton type="submit" :disabled="submitting">
        {{ submitting ? 'Enregistrement…' : 'Créer le produit' }}
      </BaseButton>
    </div>
  </form>
</template>

<style scoped>
.product-form__variant {
  align-items: end;
  margin-bottom: var(--spacing-md);
}

.product-form__remove {
  color: var(--color-danger);
  justify-self: start;
  padding-bottom: var(--spacing-sm);
}
</style>
