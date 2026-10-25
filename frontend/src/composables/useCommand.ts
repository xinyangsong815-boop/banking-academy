import { ref } from 'vue'
import { useToastStore } from '../stores/toast'

export function useCommand() {
  const pending = ref(false)
  const toast = useToastStore()
  async function run<T>(operation: () => Promise<T>, success?: string): Promise<T | undefined> {
    if (pending.value) return
    pending.value = true
    try {
      const result = await operation()
      if (success) toast.show(success)
      return result
    } catch (error) {
      toast.error(error)
      return undefined
    } finally {
      pending.value = false
    }
  }
  return { pending, run }
}

/** 网络结果不确定时保留键；只有成功或载荷改变才开始新的业务请求。 */
export function createIdempotentExecutor() {
  let fingerprint = ''
  let key = ''
  return async <T>(payload: unknown, operation: (key: string) => Promise<T>): Promise<T> => {
    const next = JSON.stringify(payload)
    if (!key || next !== fingerprint) {
      key = crypto.randomUUID()
      fingerprint = next
    }
    const result = await operation(key)
    key = ''
    return result
  }
}
