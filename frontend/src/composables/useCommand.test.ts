import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createIdempotentExecutor, useCommand } from './useCommand'

describe('业务提交', () => {
  beforeEach(() => setActivePinia(createPinia()))
  it('网络失败后重试复用键，成功后的新操作使用新键', async () => {
    const execute = createIdempotentExecutor()
    const keys: string[] = []
    const operation = vi.fn(async (key: string) => {
      keys.push(key)
      if (keys.length === 1) throw new Error('网络断开')
      return { id: 'saved' }
    })
    await expect(execute({ amount: '100' }, operation)).rejects.toThrow('网络断开')
    await execute({ amount: '100' }, operation)
    await execute({ amount: '100' }, operation)
    expect(keys[0]).toBe(keys[1])
    expect(keys[2]).not.toBe(keys[1])
  })
  it('载荷改变后生成新键', async () => {
    const execute = createIdempotentExecutor()
    const keys: string[] = []
    const operation = async (key: string) => {
      keys.push(key)
      throw new Error('超时')
    }
    await execute({ amount: '100' }, operation).catch(() => {})
    await execute({ amount: '200' }, operation).catch(() => {})
    expect(keys[0]).not.toBe(keys[1])
  })
  it('提交期间拒绝重复触发，完成后恢复按钮状态', async () => {
    const { run, pending } = useCommand()
    let resolve!: (value: string) => void
    const operation = vi.fn(
      () =>
        new Promise<string>((done) => {
          resolve = done
        }),
    )
    const first = run(operation)
    expect(pending.value).toBe(true)
    await run(operation)
    expect(operation).toHaveBeenCalledTimes(1)
    resolve('ok')
    expect(await first).toBe('ok')
    expect(pending.value).toBe(false)
  })
})
