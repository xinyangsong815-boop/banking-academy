import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request, TOKEN_KEY } from './http'

describe('HTTP 适配器', () => {
  beforeEach(() => sessionStorage.clear())
  afterEach(() => vi.unstubAllGlobals())
  it('附带会话令牌并解包业务数据', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'test-token')
    const fetchMock = vi.fn<typeof fetch>(
      async () => new Response(JSON.stringify({ code: 'OK', data: { balance: '100.01' } })),
    )
    vi.stubGlobal('fetch', fetchMock)
    expect(await request('/accounts')).toEqual({ balance: '100.01' })
    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get('Authorization')).toBe(
      'Bearer test-token',
    )
  })
  it('401 清理会话，并保留业务错误与追踪号', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'expired')
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({ code: 'UNAUTHORIZED', message: '已过期', traceId: 'trace-1' }),
            { status: 401 },
          ),
      ),
    )
    try {
      await request('/auth/me')
      throw new Error('不应成功')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError)
      expect(error).toMatchObject({ code: 'UNAUTHORIZED', traceId: 'trace-1', status: 401 })
    }
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
  })
})
