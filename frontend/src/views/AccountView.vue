<script setup lang="ts">
import { ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import LoginForm from '@/features/auth/components/LoginForm.vue'
import RegisterForm from '@/features/auth/components/RegisterForm.vue'
import { useAuth } from '@/composables/useAuth'

const { isAuthenticated, user, logout } = useAuth()
const mode = ref<'login' | 'register'>('login')
</script>

<template>
  <AppLayout>
    <h1 class="font-heading">Mon compte</h1>

    <div v-if="isAuthenticated">
      <p>Bonjour {{ user?.firstName }} {{ user?.lastName }}</p>
      <button @click="logout">Se déconnecter</button>
    </div>

    <div v-else>
      <nav class="account-tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">Connexion</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">Inscription</button>
      </nav>
      <LoginForm v-if="mode === 'login'" />
      <RegisterForm v-else />
    </div>
  </AppLayout>
</template>

<style scoped>
.account-tabs {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.account-tabs button {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding-bottom: var(--spacing-xs);
}

.account-tabs button.active {
  color: var(--color-gold);
  border-bottom: 2px solid var(--color-gold);
}
</style>
