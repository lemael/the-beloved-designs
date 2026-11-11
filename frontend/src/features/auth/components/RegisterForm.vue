<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useAuth } from '@/composables/useAuth'
import { isStrongEnoughPassword, isValidPhone } from '@/utils/validators'

const firstName = ref('')
const lastName = ref('')
const phone = ref('')
const password = ref('')
const error = ref<string | null>(null)
const { register } = useAuth()
const router = useRouter()

async function onSubmit() {
  error.value = null

  if (!isValidPhone(phone.value)) {
    error.value = 'Numéro WhatsApp invalide (format international requis, ex: +237612345678).'
    return
  }
  if (!isStrongEnoughPassword(password.value)) {
    error.value = 'Le mot de passe doit contenir au moins 8 caractères.'
    return
  }

  try {
    await register({
      phone: phone.value,
      password: password.value,
      firstName: firstName.value,
      lastName: lastName.value,
    })
    router.push('/')
  } catch {
    error.value = 'Impossible de créer le compte. Ce numéro WhatsApp est peut-être déjà utilisé.'
  }
}
</script>

<template>
  <form class="auth-form" @submit.prevent="onSubmit">
    <BaseInput v-model="firstName" label="Prénom" />
    <BaseInput v-model="lastName" label="Nom" />
    <BaseInput v-model="phone" label="Numéro WhatsApp" type="tel" placeholder="+237612345678" />
    <BaseInput v-model="password" label="Mot de passe" type="password" />
    <p v-if="error" class="auth-form__error">{{ error }}</p>
    <BaseButton type="submit">Créer mon compte</BaseButton>
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
