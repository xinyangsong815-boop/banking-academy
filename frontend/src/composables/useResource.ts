import { ref, shallowRef, onScopeDispose } from 'vue'

/** Latest-wins：快速翻页时，迟到的旧响应不能覆盖新页面。 */
export function useResource<T>(loader: () => Promise<T>) {
  const data = shallowRef<T>()
  const loading = ref(false)
  const error = ref('')
  let version = 0
  onScopeDispose(() => {
    version++
  })
  async function reload() {
    const requestVersion = ++version
    loading.value = true
    error.value = ''
    try {
      const result = await loader()
      if (version === requestVersion) data.value = result
    } catch (failure) {
      if (version === requestVersion)
        error.value = failure instanceof Error ? failure.message : '加载失败'
    } finally {
      if (version === requestVersion) loading.value = false
    }
  }
  return { data, loading, error, reload }
}
