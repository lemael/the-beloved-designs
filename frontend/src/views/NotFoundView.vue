<script setup lang="ts">
import AppLayout from '@/components/layout/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <h1 class="font-heading text-gold">404</h1>
    <p>Cette page n'existe pas.</p>
    <RouterLink to="/">Retour à l'accueil</RouterLink>
  </AppLayout>
</template>
