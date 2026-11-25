<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import { useCart } from '@/composables/useCart'
import { useCheckout } from '@/features/orders/composables/useCheckout'
import { formatPrice } from '@/utils/currency'

const { items, totalPrice, fetchCart } = useCart()
const { form, fieldErrors, status, error, cartNeedsReview, order, isSubmitting, submit } = useCheckout()

onMounted(() => fetchCart().catch(() => {}))
</script>

<template>
  <AppLayout>
    <!-- 1. Commande enregistrée : récapitulatif serveur + envoi WhatsApp -->
    <section v-if="status === 'placed' && order" class="checkout checkout--done" aria-live="polite">
      <h1 class="font-heading">Merci ! Commande n°{{ order.id }} enregistrée</h1>
      <p class="checkout__lead">
        Dernière étape : envoyez-la à la boutique sur WhatsApp. Le paiement et la livraison se règlent
        directement avec nous.
      </p>

      <a
        v-if="order.whatsappUrl"
        :href="order.whatsappUrl"
        target="_blank"
        rel="noopener"
        class="checkout__whatsapp"
      >
        Envoyer ma commande sur WhatsApp
      </a>
      <p v-else class="checkout__note">
        La boutique vous contactera sur WhatsApp au {{ order.delivery.contactPhone }}.
      </p>

      <dl class="summary">
        <div v-for="item in order.items" :key="item.productId" class="summary__row">
          <dt>{{ item.productName }} × {{ item.quantity }}</dt>
          <dd>{{ formatPrice(item.lineTotal, order.currency) }}</dd>
        </div>
        <div class="summary__row summary__row--sep">
          <dt>Sous-total</dt>
          <dd>{{ formatPrice(order.subtotal, order.currency) }}</dd>
        </div>
        <div v-if="order.firstOrderDiscount > 0" class="summary__row summary__row--discount">
          <dt>Réduction première commande (-10 %)</dt>
          <dd>-{{ formatPrice(order.firstOrderDiscount, order.currency) }}</dd>
        </div>
        <div class="summary__row">
          <dt>Livraison ({{ order.delivery.city }})</dt>
          <dd>{{ order.deliveryFee > 0 ? formatPrice(order.deliveryFee, order.currency) : 'Offerte' }}</dd>
        </div>
        <div class="summary__row summary__row--total">
          <dt>Total à payer</dt>
          <dd>{{ formatPrice(order.totalAmount, order.currency) }}</dd>
        </div>
      </dl>

      <p class="checkout__note">
        Livraison : {{ order.delivery.city }}, quartier {{ order.delivery.district }} — le livreur appellera le
        {{ order.delivery.contactPhone }}.
      </p>
      <RouterLink to="/" class="checkout__back">Continuer mes achats</RouterLink>
    </section>

    <!-- 2. Panier vide -->
    <section v-else-if="items.length === 0" class="checkout">
      <h1 class="font-heading">Finaliser la commande</h1>
      <p>Votre panier est vide.</p>
      <RouterLink to="/" class="checkout__back">Découvrir la boutique</RouterLink>
    </section>

    <!-- 3. Formulaire de livraison -->
    <section v-else class="checkout">
      <h1 class="font-heading">Finaliser la commande</h1>

      <div class="checkout__grid">
        <form class="checkout__form" novalidate @submit.prevent="submit">
          <h2 class="checkout__subtitle">Livraison</h2>
          <BaseInput
            v-model="form.deliveryCity"
            label="Ville"
            placeholder="Douala, Yaoundé…"
            :error="fieldErrors.deliveryCity"
          />
          <BaseInput
            v-model="form.deliveryDistrict"
            label="Quartier"
            placeholder="Akwa, Bastos…"
            :error="fieldErrors.deliveryDistrict"
          />
          <BaseInput
            v-model="form.contactPhone"
            label="Numéro WhatsApp que le livreur appellera"
            type="tel"
            placeholder="+237612345678"
            :error="fieldErrors.contactPhone"
          />

          <div aria-live="polite">
            <p v-if="status === 'error'" class="checkout__error" role="alert">
              {{ error }}
              <RouterLink v-if="cartNeedsReview" to="/cart">Revoir mon panier</RouterLink>
            </p>
          </div>

          <BaseButton type="submit" :disabled="isSubmitting" :aria-busy="isSubmitting">
            {{ isSubmitting ? 'Envoi…' : 'Valider la commande' }}
          </BaseButton>
        </form>

        <aside class="checkout__recap" aria-label="Récapitulatif du panier">
          <h2 class="checkout__subtitle">Votre panier</h2>
          <dl class="summary">
            <div v-for="item in items" :key="item.id" class="summary__row">
              <dt>{{ item.product.name }} × {{ item.quantity }}</dt>
              <dd>{{ formatPrice(item.lineTotal, item.product.currency) }}</dd>
            </div>
            <div class="summary__row summary__row--total">
              <dt>Sous-total</dt>
              <dd>{{ formatPrice(totalPrice) }}</dd>
            </div>
          </dl>
          <p class="checkout__note">
            -10 % sur votre première commande et frais de livraison calculés à la validation. Livraison offerte
            dès 50 000 FCFA.
          </p>
        </aside>
      </div>
    </section>
  </AppLayout>
</template>

<style scoped>
.checkout {
  max-width: 960px;
  margin-inline: auto;
}

.checkout__grid {
  display: grid;
  gap: var(--spacing-xl);
}

@media (min-width: 768px) {
  .checkout__grid {
    grid-template-columns: 1fr 1fr;
    align-items: start;
  }
}

.checkout__form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.checkout__form :deep(.base-input__error),
.checkout__error {
  /* Rouge éclairci : --color-danger manque de contraste sur fond noir */
  color: #ff6b5e;
}

.checkout__error a {
  margin-left: var(--spacing-sm);
  color: var(--color-gold);
  text-decoration: underline;
}

.checkout__form :deep(.base-button:disabled) {
  opacity: 0.6;
  cursor: wait;
}

.checkout__subtitle {
  margin: 0 0 var(--spacing-sm);
  font-size: 1.2rem;
  color: var(--color-gold-light);
}

.checkout__recap {
  padding: var(--spacing-lg);
  border: 1px solid var(--color-gold-dark);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
}

.summary {
  margin: 0;
}

.summary__row {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-md);
  padding: var(--spacing-xs) 0;
}

.summary__row dt {
  color: var(--color-text);
}

.summary__row dd {
  margin: 0;
  white-space: nowrap;
}

.summary__row--sep {
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.summary__row--discount dd {
  color: var(--color-gold-light);
}

.summary__row--total {
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-gold-dark);
  font-weight: 700;
  color: var(--color-gold);
}

.checkout__note {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.checkout__lead {
  font-size: 1.05rem;
}

.checkout--done {
  max-width: 560px;
}

.checkout--done .summary {
  margin-block: var(--spacing-lg);
  padding: var(--spacing-lg);
  border: 1px solid var(--color-gold-dark);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
}

/* Bouton WhatsApp : vert de la marque WhatsApp, reconnaissable immédiatement */
.checkout__whatsapp {
  display: inline-block;
  margin-top: var(--spacing-sm);
  padding: 12px 24px;
  border-radius: 999px;
  background-color: #25d366;
  color: #0b0b0b;
  font-weight: 700;
  text-decoration: none;
}

.checkout__whatsapp:hover {
  background-color: #3ee07a;
}

.checkout__back {
  color: var(--color-gold);
  text-decoration: underline;
}
</style>
