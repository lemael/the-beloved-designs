<script setup lang="ts">
import { ref } from 'vue'
import { RouterView } from 'vue-router'
import AdminSidebar from '@/features/admin/components/AdminSidebar.vue'
import '@/features/admin/admin.css'

// Layout du back-office : menu latéral + contenu. N'utilise pas AppLayout (pas de header/footer boutique).
const menuOpen = ref(false)
</script>

<template>
  <!-- Page de connexion : sans menu latéral -->
  <RouterView v-if="$route.meta.adminBare" />

  <div v-else class="admin-layout">
    <header class="admin-layout__mobile-bar">
      <button
        type="button"
        class="admin-layout__toggle"
        :aria-expanded="menuOpen"
        aria-controls="admin-sidebar"
        aria-label="Ouvrir le menu d'administration"
        @click="menuOpen = !menuOpen"
      >
        <span />
        <span />
        <span />
      </button>
      <span class="admin-layout__title">The Beloved Designs — Admin</span>
    </header>

    <AdminSidebar :open="menuOpen" @navigate="menuOpen = false" />

    <main class="admin-layout__content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  font-family: var(--font-body);
}

.admin-layout__content {
  flex: 1;
  min-width: 0;
  padding: var(--spacing-xl) var(--spacing-lg);
}

.admin-layout__mobile-bar {
  display: none;
}

@media (max-width: 767px) {
  .admin-layout {
    flex-direction: column;
  }

  .admin-layout__mobile-bar {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    padding: var(--spacing-sm) var(--spacing-md);
    background-color: var(--color-surface);
    border-bottom: 1px solid var(--color-border);
  }

  .admin-layout__title {
    font-family: var(--font-heading);
    color: var(--color-gold);
  }

  .admin-layout__toggle {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    width: 32px;
    height: 32px;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
  }

  .admin-layout__toggle span {
    display: block;
    height: 2px;
    background-color: var(--color-gold);
    border-radius: 1px;
  }

  .admin-layout__content {
    padding: var(--spacing-lg) var(--spacing-md);
  }
}
</style>
