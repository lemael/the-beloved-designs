<script setup lang="ts">
import { watch } from 'vue'
import { RouterView } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'

const { isAuthenticated } = useAuth()
const { fetchCart, reset } = useCart()

// Compteur du panier juste dès la première page : chargé au démarrage et à la connexion,
// oublié à la déconnexion. Un échec ici (réseau…) ne doit pas bloquer le site.
watch(
  isAuthenticated,
  (loggedIn) => {
    if (loggedIn) {
      fetchCart().catch(() => {})
    } else {
      reset()
    }
  },
  { immediate: true },
)
</script>

<template>
  <RouterView />
</template>
