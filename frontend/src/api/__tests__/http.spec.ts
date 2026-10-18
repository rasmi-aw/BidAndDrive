import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, setTokenProvider } from '@/api/http'

const fetchMock = vi.fn<typeof fetch>()

/** The ApiError a call rejected with; fails the test if it resolved. */
async function failure(call: Promise<unknown>): Promise<ApiError> {
  try {
    await call
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError)
    return error as ApiError
  }
  throw new Error('expected the call to fail')
}

function json(body: unknown, status = 200, contentType = 'application/hal+json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } })
}

describe('apiFetch', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    setTokenProvider(async () => undefined)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('returns the parsed body of a successful response', async () => {
    fetchMock.mockResolvedValue(json({ id: '1' }))

    await expect(apiFetch('/api/auctions/1')).resolves.toEqual({ id: '1' })
  })

  it('sends JSON bodies and asks for HAL or problem responses', async () => {
    fetchMock.mockResolvedValue(json({ id: '1' }, 201))

    await apiFetch('/api/auctions', { method: 'POST', body: { carName: 'Clio' } })

    const [url, init] = fetchMock.mock.calls[0]
    const headers = new Headers(init?.headers)
    expect(url).toBe('/api/auctions')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBe('{"carName":"Clio"}')
    expect(headers.get('Content-Type')).toBe('application/json')
    expect(headers.get('Accept')).toContain('application/hal+json')
    expect(headers.get('Accept')).toContain('application/problem+json')
  })

  it('adds the bearer token when the user is logged in', async () => {
    setTokenProvider(async () => 'token-123')
    fetchMock.mockResolvedValue(json({}))

    await apiFetch('/api/auctions/1')

    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get('Authorization')).toBe('Bearer token-123')
  })

  it('sends no Authorization header for anonymous calls', async () => {
    fetchMock.mockResolvedValue(json({}))

    await apiFetch('/api/auctions/running')

    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).has('Authorization')).toBe(false)
  })

  it('turns a ProblemDetail response into an ApiError with field errors', async () => {
    fetchMock.mockResolvedValue(json({
      type: 'urn:bidanddrive:problem:validation',
      title: 'Validation failed',
      status: 400,
      detail: 'The request contains invalid fields.',
      errors: [
        { field: 'carName', message: 'must not be blank' },
        { field: 'startPrice', message: 'must be greater than 0' },
      ],
    }, 400, 'application/problem+json'))

    const error = await failure(apiFetch('/api/auctions', { method: 'POST', body: {} }))

    expect(error.status).toBe(400)
    expect(error.problem.title).toBe('Validation failed')
    expect(error.message).toBe('The request contains invalid fields.')
    expect(error.fieldErrors).toEqual({ carName: 'must not be blank', startPrice: 'must be greater than 0' })
    expect(error.isType('validation')).toBe(true)
  })

  it('keeps the status of a non-problem error response', async () => {
    fetchMock.mockResolvedValue(new Response('Bad Gateway', { status: 502, statusText: 'Bad Gateway' }))

    const error = await failure(apiFetch('/api/auctions/running'))

    expect(error.status).toBe(502)
    expect(error.problem.title).toBe('Bad Gateway')
    expect(error.fieldErrors).toEqual({})
  })

  it('reports an unreachable server as a network error', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await failure(apiFetch('/api/auctions/running'))

    expect(error.status).toBe(0)
    expect(error.problem.title).toBe('Network error')
  })
})
