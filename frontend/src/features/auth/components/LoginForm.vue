<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useAuth } from '@/composables/useAuth'

const phone = ref('')
const password = ref('')
const error = ref<string | null>(null)
const { login } = useAuth()
const router = useRouter()

async function onSubmit() {
  error.value = null
  try {
    await login({ phone: phone.value, password: password.value })
    router.push('/')
  } catch {
    error.value = 'Numéro WhatsApp ou mot de passe incorrect.'
  }
}
</script>

<template>
  <form class="auth-form" @submit.prevent="onSubmit">
    <BaseInput v-model="phone" label="Numéro WhatsApp" type="tel" placeholder="+237612345678" />
    <BaseInput v-model="password" label="Mot de passe" type="password" />
    <p v-if="error" class="auth-form__error">{{ error }}</p>
    <BaseButton type="submit">Se connecter</BaseButton>
  </form>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  max-width: 360px;
}

.auth-form__error {
  color: var(--color-danger);
}
</style>
