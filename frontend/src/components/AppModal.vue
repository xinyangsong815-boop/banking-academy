<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
defineProps<{ title: string }>()
const emit = defineEmits<{ close: [] }>()
const dialog = ref<HTMLDialogElement>()
let previous: Element | null = null
onMounted(() => {
  previous = document.activeElement
  dialog.value?.showModal()
})
onUnmounted(() => {
  if (previous instanceof HTMLElement) previous.focus()
})
</script>
<template>
  <dialog
    ref="dialog"
    class="modal"
    aria-labelledby="modal-title"
    @cancel.prevent="emit('close')"
    @click="
      (event) => {
        if (event.target === dialog) emit('close')
      }
    "
  >
    <header>
      <h2 id="modal-title">{{ title }}</h2>
      <button
        class="icon-button"
        aria-label="关闭弹窗"
        @click="emit('close')"
      >
        ×
      </button>
    </header>
    <slot />
  </dialog>
</template>
