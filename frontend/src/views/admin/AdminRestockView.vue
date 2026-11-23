<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseLoader from '@/components/common/BaseLoader.vue'
import { adminService, apiErrorMessage } from '@/features/admin/services/admin.service'
import type { AdminProduct, Restock, RestockStatus } from '@/types/admin.types'
import { formatPrice } from '@/utils/currency'

const STATUS_LABELS: Record<RestockStatus, string> = { PREVU: 'Prévu', RECU: 'Reçu', ANNULE: 'Annulé' }
const STATUS_CLASSES: Record<RestockStatus, string> = {
  PREVU: 'admin-badge--warning',
  RECU: 'admin-badge--success',
  ANNULE: '',
}
// Liste déroulante des produits du formulaire (v1 : pas de recherche, catalogue de taille modeste)
const PRODUCT_PICKER_SIZE = 500

const filter = ref<RestockStatus | 'ALL'>('PREVU')
const restocks = ref<Restock[] | null>(null)
const listError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const busyId = ref<number | null>(null)

const products = ref<AdminProduct[]>([])
const showForm = ref(false)
const submitting = ref(false)
const formError = ref<string | null>(null)
const success = ref<string | null>(null)

interface ItemRow {
  productId: number | null
  variantId: number | null
  quantity: number | null
  unitCost: number | null
}

const form = reactive({
  expectedDate: '',
  supplier: '',
  notes: '',
  items: [] as ItemRow[],
})

function emptyRow(): ItemRow {
  return { productId: null, variantId: null, quantity: 1, unitCost: null }
}

function resetForm() {
  form.expectedDate = ''
  form.supplier = ''
  form.notes = ''
  form.items = [emptyRow()]
}

const productById = computed(() => new Map(products.value.map((p) => [p.id, p])))

// Deux produits du même nom : on affiche aussi le slug pour les distinguer dans la liste
const duplicateNames = computed(() => {
  const seen = new Set<string>()
  const duplicates = new Set<string>()
  for (const p of products.value) {
    if (seen.has(p.name)) duplicates.add(p.name)
    seen.add(p.name)
  }
  return duplicates
})

function variantsOf(productId: number | null) {
  return productId ? (productById.value.get(productId)?.variants ?? []) : []
}

function variantLabel(v: { size: string | null; color: string | null }) {
  return [v.size, v.color].filter(Boolean).join(' / ')
}

function formatDate(iso: string) {
  return new Date(iso.length === 10 ? `${iso}T00:00:00` : iso).toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

function daysUntil(isoDate: string) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((new Date(`${isoDate}T00:00:00`).getTime() - today.getTime()) / 86_400_000)
}

function dueLabel(r: Restock) {
  if (r.status !== 'PREVU') return null
  const d = daysUntil(r.expectedDate)
  if (d < 0) return { text: `En retard de ${-d} j`, cls: 'admin-badge--danger' }
  if (d === 0) return { text: "Aujourd'hui", cls: 'admin-badge--warning' }
  return { text: `Dans ${d} j`, cls: '' }
}

async function loadRestocks() {
  listError.value = null
  try {
    const page = await adminService.listRestocks({
      status: filter.value === 'ALL' ? undefined : filter.value,
      size: 100,
    })
    restocks.value = page.content
  } catch (e) {
    listError.value = apiErrorMessage(e, 'Impossible de charger les ravitaillements.')
  }
}

async function loadProducts() {
  try {
    const page = await adminService.listProducts({ size: PRODUCT_PICKER_SIZE })
    products.value = page.content
  } catch (e) {
    formError.value = apiErrorMessage(e, 'Impossible de charger la liste des produits.')
  }
}

function openForm() {
  resetForm()
  formError.value = null
  success.value = null
  showForm.value = true
  if (products.value.length === 0) loadProducts()
}

async function submit() {
  formError.value = null
  if (!form.expectedDate) {
    formError.value = 'La date prévue est obligatoire.'
    return
  }
  const items = form.items.filter((i) => i.productId !== null)
  if (items.length === 0) {
    formError.value = 'Ajoutez au moins un produit.'
    return
  }
  if (items.some((i) => !i.quantity || i.quantity < 1 || !Number.isInteger(i.quantity))) {
    formError.value = 'Chaque quantité doit être un nombre entier supérieur à 0.'
    return
  }

  submitting.value = true
  try {
    await adminService.createRestock({
      expectedDate: form.expectedDate,
      supplier: form.supplier.trim() || undefined,
      notes: form.notes.trim() || undefined,
      items: items.map((i) => ({
        productId: i.productId!,
        variantId: i.variantId,
        quantity: i.quantity!,
        unitCost: i.unitCost ?? null,
      })),
    })
    showForm.value = false
    success.value = 'Ravitaillement enregistré.'
    await loadRestocks()
  } catch (e) {
    formError.value = apiErrorMessage(e, "Le ravitaillement n'a pas pu être enregistré.")
  } finally {
    submitting.value = false
  }
}

async function act(restock: Restock, action: 'receive' | 'cancel') {
  const question =
    action === 'receive'
      ? `Confirmer la réception ? Le stock de ${restock.totalQuantity} article(s) sera augmenté. Cette action est définitive.`
      : 'Annuler ce ravitaillement ? Cette action est définitive.'
  if (!window.confirm(question)) return

  actionError.value = null
  success.value = null
  busyId.value = restock.id
  try {
    if (action === 'receive') {
      await adminService.receiveRestock(restock.id)
      success.value = 'Réception enregistrée : le stock a été mis à jour.'
    } else {
      await adminService.cancelRestock(restock.id)
      success.value = 'Ravitaillement annulé.'
    }
    await loadRestocks()
  } catch (e) {
    actionError.value = apiErrorMessage(e, "L'action a échoué.")
  } finally {
    busyId.value = null
  }
}

onMounted(loadRestocks)
</script>

<template>
  <section>
    <div class="restock__header">
      <h1 class="admin-page__title">Prochain ravitaillement</h1>
      <BaseButton v-if="!showForm" @click="openForm">+ Planifier un ravitaillement</BaseButton>
    </div>

    <p v-if="success" class="admin-success restock__message" role="status">{{ success }}</p>

    <!-- Formulaire de création -->
    <form v-if="showForm" class="admin-card admin-form" novalidate @submit.prevent="submit">
      <h2 class="admin-card__title">Nouveau ravitaillement</h2>
      <div class="admin-form__row">
        <label class="admin-field">
          <span>Date prévue *</span>
          <input v-model="form.expectedDate" type="date" required />
        </label>
        <label class="admin-field">
          <span>Fournisseur</span>
          <input v-model="form.supplier" type="text" maxlength="150" />
        </label>
      </div>
      <label class="admin-field">
        <span>Notes</span>
        <textarea v-model="form.notes" rows="2" />
      </label>

      <h3 class="restock__subtitle">Produits attendus</h3>
      <div v-for="(item, i) in form.items" :key="i" class="admin-form__row restock__item">
        <label class="admin-field restock__product">
          <span>Produit</span>
          <select v-model="item.productId" @change="item.variantId = null">
            <option :value="null" disabled>Choisir…</option>
            <option v-for="p in products" :key="p.id" :value="p.id">
              {{ p.name }}<template v-if="duplicateNames.has(p.name)"> · {{ p.slug }}</template>
              (stock : {{ p.stockQuantity }})
            </option>
          </select>
        </label>
        <label v-if="variantsOf(item.productId).length" class="admin-field">
          <span>Variante</span>
          <select v-model="item.variantId">
            <option :value="null">Toutes / aucune</option>
            <option v-for="v in variantsOf(item.productId)" :key="v.id" :value="v.id">{{ variantLabel(v) }}</option>
          </select>
        </label>
        <label class="admin-field">
          <span>Quantité</span>
          <input v-model.number="item.quantity" type="number" min="1" step="1" />
        </label>
        <label class="admin-field">
          <span>Prix d'achat unitaire (FCFA)</span>
          <input v-model.number="item.unitCost" type="number" min="0" step="1" />
        </label>
        <button
          v-if="form.items.length > 1"
          type="button"
          class="admin-link-button restock__remove"
          @click="form.items.splice(i, 1)"
        >
          Retirer
        </button>
      </div>
      <div>
        <BaseButton variant="ghost" @click="form.items.push(emptyRow())">+ Ajouter un produit</BaseButton>
      </div>

      <p v-if="formError" class="admin-error" role="alert">{{ formError }}</p>
      <div class="admin-actions">
        <BaseButton type="submit" :disabled="submitting">{{ submitting ? 'Enregistrement…' : 'Enregistrer' }}</BaseButton>
        <BaseButton variant="ghost" @click="showForm = false">Annuler</BaseButton>
      </div>
    </form>

    <!-- Liste -->
    <div class="admin-card">
      <div class="admin-actions restock__filters">
        <label class="admin-check"><input v-model="filter" type="radio" value="PREVU" @change="loadRestocks" /> À venir</label>
        <label class="admin-check"><input v-model="filter" type="radio" value="RECU" @change="loadRestocks" /> Reçus</label>
        <label class="admin-check"><input v-model="filter" type="radio" value="ANNULE" @change="loadRestocks" /> Annulés</label>
        <label class="admin-check"><input v-model="filter" type="radio" value="ALL" @change="loadRestocks" /> Tous</label>
      </div>

      <p v-if="listError" class="admin-error">{{ listError }}</p>
      <p v-if="actionError" class="admin-error" role="alert">{{ actionError }}</p>
      <BaseLoader v-if="!restocks && !listError" />
      <p v-else-if="restocks && restocks.length === 0" class="admin-muted">Aucun ravitaillement.</p>

      <article v-for="r in restocks ?? []" :key="r.id" class="restock__entry">
        <header class="restock__entry-header">
          <div>
            <strong>{{ formatDate(r.expectedDate) }}</strong>
            <span v-if="r.supplier" class="admin-muted"> — {{ r.supplier }}</span>
          </div>
          <div class="admin-actions">
            <span class="admin-badge" :class="STATUS_CLASSES[r.status]">{{ STATUS_LABELS[r.status] }}</span>
            <span v-if="dueLabel(r)" class="admin-badge" :class="dueLabel(r)!.cls">{{ dueLabel(r)!.text }}</span>
          </div>
        </header>

        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Produit</th>
                <th class="num">Quantité</th>
                <th class="num">Prix d'achat</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in r.items" :key="item.id">
                <td>
                  {{ item.productName }}
                  <span v-if="item.variantLabel" class="admin-muted">({{ item.variantLabel }})</span>
                </td>
                <td class="num">{{ item.quantity }}</td>
                <td class="num">{{ item.unitCost !== null ? formatPrice(item.unitCost) : '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <footer class="restock__entry-footer">
          <span class="admin-muted">
            {{ r.totalQuantity }} article(s)<template v-if="r.totalCost !== null"> · coût {{ formatPrice(r.totalCost) }}</template>
            <template v-if="r.receivedAt"> · reçu le {{ formatDate(r.receivedAt) }}</template>
          </span>
          <div v-if="r.status === 'PREVU'" class="admin-actions">
            <BaseButton :disabled="busyId === r.id" @click="act(r, 'receive')">Marquer comme reçu</BaseButton>
            <BaseButton variant="ghost" :disabled="busyId === r.id" @click="act(r, 'cancel')">Annuler</BaseButton>
          </div>
        </footer>
        <p v-if="r.notes" class="admin-muted restock__notes">{{ r.notes }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.restock__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--spacing-md);
}

.restock__message {
  margin-bottom: var(--spacing-md);
}

.restock__subtitle {
  margin: var(--spacing-sm) 0 0;
  font-size: 0.95rem;
  color: var(--color-gold-light);
}

.restock__item {
  align-items: end;
}

.restock__product {
  grid-column: span 2;
}

.restock__remove {
  color: var(--color-danger);
  justify-self: start;
  padding-bottom: var(--spacing-sm);
}

.restock__filters {
  margin-bottom: var(--spacing-md);
}

.restock__entry {
  border-top: 1px solid var(--color-border);
  padding-top: var(--spacing-md);
  margin-top: var(--spacing-md);
}

.restock__entry-header,
.restock__entry-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-sm);
}

.restock__entry-footer {
  margin-top: var(--spacing-sm);
}

.restock__notes {
  margin: var(--spacing-sm) 0 0;
  font-style: italic;
}

@media (max-width: 767px) {
  .restock__product {
    grid-column: auto;
  }
}
</style>
