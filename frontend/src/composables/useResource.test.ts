import { effectScope } from 'vue'
import { describe, expect, it } from 'vitest'
import { useResource } from './useResource'

describe('请求竞态', () => {
  it('较慢的旧响应不能覆盖新响应', async () => {
    const resolvers: ((value: string) => void)[] = []
    const scope = effectScope()
    const resource = scope.run(() =>
      useResource(() => new Promise<string>((resolve) => resolvers.push(resolve))),
    )!
    const oldRequest = resource.reload()
    const newRequest = resource.reload()
    resolvers[1]('新页')
    await newRequest
    resolvers[0]('旧页')
    await oldRequest
    expect(resource.data.value).toBe('新页')
    expect(resource.loading.value).toBe(false)
    scope.stop()
  })
  it('页面离开后忽略响应', async () => {
    let resolve!: (value: number) => void
    const scope = effectScope()
    const resource = scope.run(() =>
      useResource(
        () =>
          new Promise<number>((done) => {
            resolve = done
          }),
      ),
    )!
    const request = resource.reload()
    scope.stop()
    resolve(123)
    await request
    expect(resource.data.value).toBeUndefined()
  })
})
