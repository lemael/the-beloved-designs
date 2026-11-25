<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { ADMIN_PRODUCT_LISTS } from '@/features/admin/admin.config'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ navigate: [] }>()

const { user, logout } = useAuth()
const router = useRouter()

function onLogout() {
  logout()
  emit('navigate')
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <aside id="admin-sidebar" class="admin-sidebar" :class="{ 'is-open': open }">
    <div class="admin-sidebar__brand">
      <span class="admin-sidebar__logo">TB</span>
      <span>Administration</span>
    </div>
    <p v-if="user" class="admin-sidebar__user">{{ user.firstName }} {{ user.lastName }}</p>

    <nav class="admin-sidebar__nav" aria-label="Menu administration">
      <RouterLink :to="{ name: 'admin-dashboard' }" @click="emit('navigate')">Tableau de bord</RouterLink>
      <RouterLink :to="{ name: 'admin-payments' }" @click="emit('navigate')">Confirmation paiement</RouterLink>
      <RouterLink :to="{ name: 'admin-product-new' }" @click="emit('navigate')">Créer un nouveau produit</RouterLink>

      <p class="admin-sidebar__section">Produits</p>
      <RouterLink
        v-for="list in ADMIN_PRODUCT_LISTS"
        :key="list.key"
        :to="{ name: 'admin-products', query: { list: list.key } }"
        :class="{ 'is-active': $route.name === 'admin-products' && $route.query.list === list.key }"
        exact-active-class=""
        active-class=""
        @click="emit('navigate')"
      >
        {{ list.menuLabel }}
      </RouterLink>

      <p class="admin-sidebar__section">Suivi</p>
      <RouterLink :to="{ name: 'admin-stock' }" @click="emit('navigate')">Bilan actuel du stock</RouterLink>
      <RouterLink :to="{ name: 'admin-restocks' }" @click="emit('navigate')">Prochain ravitaillement</RouterLink>
      <RouterLink :to="{ name: 'admin-yearly-report' }" @click="emit('navigate')">Bilan de l'année</RouterLink>
    </nav>

    <div class="admin-sidebar__footer">
      <RouterLink to="/" class="admin-sidebar__shop" @click="emit('navigate')">Voir la boutique</RouterLink>
      <button type="button" class="admin-sidebar__logout" @click="onLogout">Se déconnecter</button>
    </div>
  </aside>
</template>

<style scoped>
.admin-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  width: 260px;
  flex-shrink: 0;
  background-color: var(--color-surface);
  border-right: 1px solid var(--color-border);
  padding: var(--spacing-lg) var(--spacing-md);
  min-height: 100vh;
  position: sticky;
  top: 0;
  align-self: flex-start;
  max-height: 100vh;
  overflow-y: auto;
}

.admin-sidebar__brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-family: var(--font-heading);
  color: var(--color-gold);
  font-size: 1.1rem;
}

.admin-sidebar__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-gold);
  border-radius: 50%;
  font-weight: 700;
}

.admin-sidebar__user {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.admin-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-sidebar__nav a {
  color: var(--color-text);
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
  border-left: 3px solid transparent;
}

.admin-sidebar__nav a:hover {
  background-color: var(--color-background);
  color: var(--color-gold-light);
}

.admin-sidebar__nav a.router-exact-active,
.admin-sidebar__nav a.is-active {
  border-left-color: var(--color-gold);
  color: var(--color-gold);
  background-color: var(--color-background);
}

.admin-sidebar__section {
  margin: var(--spacing-md) 0 var(--spacing-xs);
  padding-left: var(--spacing-sm);
  color: var(--color-text-muted);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.admin-sidebar__footer {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border);
}

.admin-sidebar__shop {
  padding: var(--spacing-sm);
}

.admin-sidebar__logout {
  background: none;
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  cursor: pointer;
  font-weight: 600;
}

.admin-sidebar__logout:hover {
  background-color: var(--color-danger);
  color: var(--color-text);
}

/* Mobile : menu replié, ouvert par le bouton hamburger d'AdminLayout */
@media (max-width: 767px) {
  .admin-sidebar {
    display: none;
    position: static;
    width: 100%;
    min-height: auto;
    max-height: none;
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }

  .admin-sidebar.is-open {
    display: flex;
  }
}
</style>
