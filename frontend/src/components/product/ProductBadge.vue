<script setup lang="ts">
import BaseBadge from '@/components/common/BaseBadge.vue'

defineProps<{ featured?: boolean }>()
</script>

<template>
  <BaseBadge v-if="featured" variant="new">Nouveau</BaseBadge>
</template>
