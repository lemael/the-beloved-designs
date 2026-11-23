<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useAuthStore } from '@/stores/auth.store'
import { isValidPhone } from '@/utils/validators'

const phone = ref('')
const password = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

function redirectTarget(): string {
  const redirect = route.query.redirect
  // Seulement une route interne du back-office (pas d'URL externe)
  return typeof redirect === 'string' && redirect.startsWith('/admin/') ? redirect : '/admin/dashboard'
}

onMounted(() => {
  if (authStore.isAdmin) {
    router.replace(redirectTarget())
  }
})

async function onSubmit() {
  error.value = null
  if (!isValidPhone(phone.value)) {
    error.value = 'Numéro invalide (format international, ex : +237612345678).'
    return
  }
  if (!password.value) {
    error.value = 'Le mot de passe est obligatoire.'
    return
  }

  submitting.value = true
  try {
    await authStore.login({ phone: phone.value, password: password.value })
  } catch {
    error.value = 'Numéro ou mot de passe incorrect.'
    submitting.value = false
    return
  }
  submitting.value = false

  if (!authStore.isAdmin) {
    // Compte valide mais sans rôle ADMIN : on ne garde pas la session ouverte ici.
    authStore.logout()
    error.value = "Accès refusé : ce compte n'a pas les droits d'administration."
    return
  }
  router.replace(redirectTarget())
}
</script>

<template>
  <div class="admin-login">
    <form class="admin-login__card" novalidate @submit.prevent="onSubmit">
      <div class="admin-login__logo">TB</div>
      <h1 class="admin-login__title">Espace administration</h1>
      <p class="admin-muted admin-login__subtitle">The Beloved Designs</p>

      <BaseInput v-model="phone" label="Numéro de téléphone" type="tel" placeholder="+237612345678" />
      <BaseInput v-model="password" label="Mot de passe" type="password" />

      <p v-if="error" class="admin-error" role="alert">{{ error }}</p>
      <BaseButton type="submit" :disabled="submitting">
        {{ submitting ? 'Connexion…' : 'Se connecter' }}
      </BaseButton>
      <RouterLink to="/" class="admin-login__back">← Retour à la boutique</RouterLink>
    </form>
  </div>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-md);
  font-family: var(--font-body);
}

.admin-login__card {
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-top: 3px solid var(--color-gold);
  border-radius: var(--radius-md);
  padding: var(--spacing-xl) var(--spacing-lg);
}

.admin-login__logo {
  align-self: center;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 1px solid var(--color-gold);
  border-radius: 50%;
  color: var(--color-gold);
  font-family: var(--font-heading);
  font-size: 1.3rem;
  font-weight: 700;
}

.admin-login__title {
  margin: 0;
  text-align: center;
  font-family: var(--font-heading);
  color: var(--color-gold);
  font-size: 1.4rem;
}

.admin-login__subtitle {
  margin: calc(-1 * var(--spacing-sm)) 0 var(--spacing-sm);
  text-align: center;
}

.admin-login__back {
  text-align: center;
  font-size: 0.85rem;
}
</style>
