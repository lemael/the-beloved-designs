<script setup lang="ts">
import { onMounted } from 'vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { usePaymentConfirmation } from '@/features/admin/composables/usePaymentConfirmation'
import { PAYMENT_METHOD_LABELS, type PaymentMethod } from '@/types/admin.types'
import { formatPrice } from '@/utils/currency'

const {
  categories,
  productsByCategory,
  loadCategories,
  orderNumber,
  loadedOrder,
  loadingOrder,
  orderError,
  loadOrder,
  form,
  touched,
  addLine,
  removeLine,
  changeCategory,
  unitPrice,
  subtotal,
  firstOrderDiscount,
  total,
  status,
  error,
  result,
  submit,
  reset,
} = usePaymentConfirmation()

const methods = Object.entries(PAYMENT_METHOD_LABELS) as [PaymentMethod, string][]
const KNOWN_CITIES = ['Douala', 'Yaoundé']

function orderedProductName(productId: number | null) {
  return loadedOrder.value?.items.find((i) => i.productId === productId)?.productName
}

/** Produit de la commande qui n'est plus en vente : on le garde dans la liste pour pouvoir le confirmer. */
function missingFromCatalog(slug: string | null, productId: number | null) {
  if (productId === null) return false
  return !(slug && productsByCategory[slug]?.some((p) => p.id === productId))
}

function formatDay(iso: string) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
}

onMounted(() => loadCategories().catch(() => (error.value = 'Impossible de charger les catégories.')))
</script>

<template>
  <section class="payment">
    <h1 class="admin-page__title">Confirmation paiement</h1>

    <!-- ===== Paiement enregistré : reçu à envoyer ===== -->
    <article v-if="status === 'done' && result" class="admin-card payment__done" aria-live="polite">
      <h2 class="admin-card__title">Paiement enregistré — commande n°{{ result.orderId }} payée</h2>
      <p class="admin-muted">
        {{ PAYMENT_METHOD_LABELS[result.paymentMethod] }} · {{ formatDay(result.paidOn) }} · client
        {{ result.customerWhatsapp }}
      </p>

      <a :href="result.receiptWhatsappUrl" target="_blank" rel="noopener" class="payment__whatsapp">
        Envoyer le reçu au client sur WhatsApp
      </a>

      <dl class="payment__summary">
        <div v-for="item in result.items" :key="item.productId">
          <dt>{{ item.productName }} × {{ item.quantity }}</dt>
          <dd>{{ formatPrice(item.lineTotal) }}</dd>
        </div>
        <div class="payment__summary-sep">
          <dt>Sous-total</dt>
          <dd>{{ formatPrice(result.subtotal) }}</dd>
        </div>
        <div v-if="result.firstOrderDiscount > 0">
          <dt>Réduction première commande</dt>
          <dd>-{{ formatPrice(result.firstOrderDiscount) }}</dd>
        </div>
        <div v-if="result.discount > 0">
          <dt>Réduction</dt>
          <dd>-{{ formatPrice(result.discount) }}</dd>
        </div>
        <div>
          <dt>Livraison<template v-if="result.deliveryCity"> ({{ result.deliveryCity }})</template></dt>
          <dd>{{ result.deliveryFee > 0 ? formatPrice(result.deliveryFee) : 'Offerte' }}</dd>
        </div>
        <div class="payment__summary-total">
          <dt>Total</dt>
          <dd>{{ formatPrice(result.total) }}</dd>
        </div>
        <div class="payment__summary-total">
          <dt>Montant payé</dt>
          <dd>{{ formatPrice(result.amountPaid) }}</dd>
        </div>
      </dl>

      <BaseButton variant="ghost" @click="reset">Nouvelle confirmation de paiement</BaseButton>
    </article>

    <!-- ===== Formulaire ===== -->
    <form v-else class="admin-form" novalidate @submit.prevent="submit">
      <!-- Commande du site -->
      <div class="admin-card admin-form">
        <h2 class="admin-card__title">Commande du site</h2>
        <p class="admin-muted payment__hint">
          Saisissez le n° de commande reçu sur WhatsApp pour pré-remplir le formulaire. Laissez vide pour une vente
          directe (WhatsApp, boutique).
        </p>
        <div class="payment__order">
          <label class="admin-field">
            <span>N° de commande</span>
            <input
              v-model="orderNumber"
              type="text"
              inputmode="numeric"
              placeholder="ex. 12"
              @keydown.enter.prevent="loadOrder"
            />
          </label>
          <BaseButton variant="ghost" :disabled="loadingOrder" @click="loadOrder">
            {{ loadingOrder ? 'Chargement…' : 'Charger la commande' }}
          </BaseButton>
          <button v-if="loadedOrder" type="button" class="admin-link-button" @click="reset">
            Passer en vente directe
          </button>
        </div>
        <p v-if="orderError" class="admin-error" role="alert">{{ orderError }}</p>
        <p v-else-if="loadedOrder" class="admin-success" role="status">
          Commande n°{{ loadedOrder.orderId }} chargée ({{ formatPrice(loadedOrder.total) }}). Vous pouvez corriger
          produits et quantités : le stock sera ajusté.
        </p>
        <p v-else class="payment__mode">Vente directe : une commande payée sera créée et le stock diminué.</p>
      </div>

      <!-- Client -->
      <div class="admin-card admin-form">
        <h2 class="admin-card__title">Client</h2>
        <label class="admin-field payment__phone">
          <span>Numéro WhatsApp du client *</span>
          <input v-model="form.customerWhatsapp" type="tel" placeholder="+237612345678" autocomplete="off" />
        </label>
      </div>

      <!-- Produits -->
      <div class="admin-card admin-form">
        <h2 class="admin-card__title">Produits commandés</h2>
        <div v-for="(line, index) in form.lines" :key="line.key" class="payment__line">
          <span class="payment__line-number" aria-hidden="true">{{ index + 1 }}</span>
          <label class="admin-field">
            <span>Catégorie</span>
            <select v-model="line.categorySlug" @change="changeCategory(line, line.categorySlug)">
              <option :value="null" disabled>Choisir…</option>
              <option v-for="c in categories" :key="c.slug" :value="c.slug">{{ c.name }}</option>
            </select>
          </label>
          <label class="admin-field">
            <span>Produit</span>
            <select v-model="line.productId" :disabled="!line.categorySlug">
              <option :value="null" disabled>Choisir…</option>
              <option v-if="missingFromCatalog(line.categorySlug, line.productId)" :value="line.productId">
                {{ orderedProductName(line.productId) ?? 'Produit de la commande' }} (plus en vente)
              </option>
              <option v-for="p in productsByCategory[line.categorySlug ?? ''] ?? []" :key="p.id" :value="p.id">
                {{ p.name }} — {{ formatPrice(p.promoPrice ?? p.price) }} (stock : {{ p.stockQuantity }})
              </option>
            </select>
          </label>
          <label class="admin-field payment__qty">
            <span>Quantité</span>
            <input v-model.number="line.quantity" type="number" min="1" step="1" />
          </label>
          <p class="payment__line-total">
            <span class="admin-muted">Sous-total</span>
            {{ line.productId !== null ? formatPrice(unitPrice(line) * (line.quantity || 0)) : '—' }}
          </p>
          <button
            type="button"
            class="admin-link-button payment__remove"
            :aria-label="`Retirer le produit ${index + 1}`"
            @click="removeLine(line.key)"
          >
            Retirer
          </button>
        </div>
        <div>
          <BaseButton variant="ghost" @click="addLine">+ Ajouter un produit</BaseButton>
        </div>
      </div>

      <!-- Livraison et paiement -->
      <div class="admin-card admin-form">
        <h2 class="admin-card__title">Livraison et paiement</h2>
        <div class="admin-form__row">
          <label class="admin-field">
            <span>Ville</span>
            <input v-model="form.deliveryCity" type="text" list="payment-cities" placeholder="Douala, Yaoundé…" />
            <datalist id="payment-cities">
              <option v-for="c in KNOWN_CITIES" :key="c" :value="c" />
            </datalist>
          </label>
          <label class="admin-field">
            <span>Quartier</span>
            <input v-model="form.deliveryDistrict" type="text" placeholder="Akwa, Bastos…" />
          </label>
          <label class="admin-field">
            <span>Frais de livraison (FCFA)</span>
            <input
              v-model.number="form.deliveryFee"
              type="number"
              min="0"
              step="1"
              @input="touched.deliveryFee = true"
            />
          </label>
        </div>
        <div class="admin-form__row">
          <label class="admin-field">
            <span>Réduction (FCFA)</span>
            <input v-model.number="form.discount" type="number" min="0" step="1" />
          </label>
          <label class="admin-field">
            <span>Mode de paiement *</span>
            <select v-model="form.paymentMethod">
              <option :value="null" disabled>Choisir…</option>
              <option v-for="[value, label] in methods" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <label class="admin-field">
            <span>Date du paiement *</span>
            <input v-model="form.paidOn" type="date" />
          </label>
        </div>

        <dl class="payment__summary">
          <div>
            <dt>Sous-total</dt>
            <dd>{{ formatPrice(subtotal) }}</dd>
          </div>
          <div v-if="firstOrderDiscount > 0">
            <dt>Réduction première commande (-10 %)</dt>
            <dd>-{{ formatPrice(firstOrderDiscount) }}</dd>
          </div>
          <div v-if="form.discount > 0">
            <dt>Réduction</dt>
            <dd>-{{ formatPrice(form.discount) }}</dd>
          </div>
          <div>
            <dt>Livraison</dt>
            <dd>{{ form.deliveryFee > 0 ? formatPrice(form.deliveryFee) : 'Offerte' }}</dd>
          </div>
          <div class="payment__summary-total">
            <dt>Total</dt>
            <dd>{{ formatPrice(total) }}</dd>
          </div>
        </dl>

        <label class="admin-field payment__paid">
          <span>Montant payé (FCFA) *</span>
          <input v-model.number="form.amountPaid" type="number" min="0" step="1" @input="touched.amountPaid = true" />
        </label>
      </div>

      <p v-if="error" class="admin-error payment__error" role="alert">{{ error }}</p>
      <div class="admin-actions">
        <BaseButton type="submit" :disabled="status === 'submitting'" :aria-busy="status === 'submitting'">
          {{ status === 'submitting' ? 'Enregistrement…' : 'Confirmer le paiement' }}
        </BaseButton>
      </div>
    </form>
  </section>
</template>

<style scoped>
.payment {
  max-width: 960px;
}

.payment__hint {
  margin: 0;
}

.payment__order {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--spacing-md);
}

.payment__order .admin-field {
  width: 160px;
}

.payment__mode {
  margin: 0;
  color: var(--color-gold-light);
}

.payment__phone {
  max-width: 320px;
}

/* Sous-formulaire produit : n° | catégorie | produit | quantité | sous-total | retirer */
.payment__line {
  display: grid;
  grid-template-columns: 28px minmax(140px, 1fr) minmax(200px, 2fr) 90px minmax(110px, auto) auto;
  align-items: end;
  gap: var(--spacing-md);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px dashed var(--color-border);
}

.payment__line-number {
  align-self: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: var(--color-gold-dark);
  color: #0b0b0b;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
}

.payment__line-total {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  margin: 0;
  white-space: nowrap;
}

.payment__line-total .admin-muted {
  font-size: 0.85rem;
}

.payment__remove {
  padding-bottom: var(--spacing-sm);
}

@media (max-width: 760px) {
  .payment__line {
    grid-template-columns: 1fr 1fr;
  }

  .payment__line-number {
    grid-column: 1 / -1;
  }
}

.payment__summary {
  margin: 0;
  padding: var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.payment__summary > div {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-md);
  padding: 2px 0;
}

.payment__summary dd {
  margin: 0;
  white-space: nowrap;
}

.payment__summary-sep {
  margin-top: var(--spacing-xs);
  padding-top: var(--spacing-xs) !important;
  border-top: 1px solid var(--color-border);
}

.payment__summary-total {
  font-weight: 700;
  color: var(--color-gold);
}

.payment__paid {
  max-width: 240px;
}

.payment__error,
.payment :deep(.admin-error) {
  /* --color-danger manque de contraste sur fond noir */
  color: #ff6b5e;
}

.payment__done {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--spacing-md);
}

.payment__done .payment__summary {
  align-self: stretch;
}

.payment__whatsapp {
  display: inline-block;
  padding: 12px 24px;
  border-radius: 999px;
  background-color: #25d366;
  color: #0b0b0b;
  font-weight: 700;
  text-decoration: none;
}

.payment__whatsapp:hover {
  background-color: #3ee07a;
}
</style>
