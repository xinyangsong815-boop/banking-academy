<script setup lang="ts">
import { useToastStore } from './stores/toast'
const toast = useToastStore()
</script>
<template>
  <RouterView />
  <div
    class="toast-stack"
    aria-live="polite"
  >
    <div
      v-for="item in toast.items"
      :key="item.id"
      class="toast"
      :class="item.kind"
    >
      {{ item.message }}
    </div>
  </div>
</template>
