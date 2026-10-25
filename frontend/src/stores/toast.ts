import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ApiError } from '../api/http'

export const useToastStore = defineStore('toast', () => {
  const items = ref<{ id: number; message: string; kind: 'success' | 'error' }[]>([])
  let nextId = 0
  function show(message: string, kind: 'success' | 'error' = 'success') {
    const id = ++nextId
    items.value.push({ id, message, kind })
    setTimeout(
      () => {
        items.value = items.value.filter((item) => item.id !== id)
      },
      kind === 'error' ? 12_000 : 5000,
    )
  }
  function error(error: unknown) {
    const suffix = error instanceof ApiError && error.traceId ? `（追踪号 ${error.traceId}）` : ''
    show((error instanceof Error ? error.message : '操作失败') + suffix, 'error')
  }
  return { items, show, error }
})
