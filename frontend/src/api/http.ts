export const TOKEN_KEY = 'academy.session'
interface Envelope<T> {
  code: string
  message: string
  data: T
  traceId: string
}

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
    public traceId?: string,
  ) {
    super(message)
  }
}

/** 传输适配器：组件只处理业务对象，不需要理解响应信封或认证头。 */
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body) headers.set('Content-Type', 'application/json')
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 15_000)
  try {
    const response = await fetch(`/api${path}`, { ...options, headers, signal: controller.signal })
    const body = (await response.json()) as Envelope<T>
    if (!response.ok || body.code !== 'OK') {
      if (response.status === 401 && path !== '/auth/login') {
        sessionStorage.removeItem(TOKEN_KEY)
        window.dispatchEvent(new Event('session-expired'))
      }
      throw new ApiError(body.code, body.message, response.status, body.traceId)
    }
    return body.data
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError(
      'NETWORK_ERROR',
      '连接失败或超时；资金操作请保持原表单重试，以复用幂等键。',
      0,
    )
  } finally {
    clearTimeout(timeout)
  }
}

export function post<T>(path: string, payload?: unknown, key?: string) {
  return request<T>(path, {
    method: 'POST',
    body: payload === undefined ? undefined : JSON.stringify(payload),
    headers: key ? { 'Idempotency-Key': key } : undefined,
  })
}

export function put<T>(path: string, payload?: unknown) {
  return request<T>(path, {
    method: 'PUT',
    body: payload === undefined ? undefined : JSON.stringify(payload),
  })
}
