<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppNavbar from './AppNavbar.vue'
import AppFooter from './AppFooter.vue'
import MobileNavbar from './MobileNavbar.vue'
</script>

<template>
  <div class="app-layout">
    <AppHeader />
    <AppNavbar />
    <main class="container app-layout__content">
      <slot />
    </main>
    <AppFooter />
    <MobileNavbar />
  </div>
</template>

<style scoped>
.app-layout__content {
  min-height: 60vh;
  padding-block: var(--spacing-lg);
  padding-bottom: 80px;
}
</style>
