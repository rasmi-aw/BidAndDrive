import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useRunningAuctions } from '@/composables/useRunningAuctions'
import { auction, halResponse, page, problemResponse, request } from '@/testing/fixtures'

const fetchMock = vi.fn<typeof fetch>()

describe('useRunningAuctions', () => {
  beforeEach(() => vi.stubGlobal('fetch', fetchMock))

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('loads the first page from the public entry point', async () => {
    fetchMock.mockResolvedValueOnce(halResponse(page('auctions', [auction({ id: 'a1' }), auction({ id: 'a2' })], 0, 2)))
    const running = useRunningAuctions(2)

    await running.load()

    expect(request(fetchMock, 0).url).toBe('/api/auctions/running?page=0&size=2')
    expect(running.auctions.value.map((a) => a.id)).toEqual(['a1', 'a2'])
    expect(running.page.value?.page.totalPages).toBe(2)
    expect(running.hasNext.value).toBe(true)
    expect(running.hasPrev.value).toBe(false)
  })

  it('pages by following the next and prev links', async () => {
    fetchMock
      .mockResolvedValueOnce(halResponse(page('auctions', [auction({ id: 'a1' })], 0, 2)))
      .mockResolvedValueOnce(halResponse(page('auctions', [auction({ id: 'a3' })], 1, 2)))
      .mockResolvedValueOnce(halResponse(page('auctions', [auction({ id: 'a1' })], 0, 2)))
    const running = useRunningAuctions(2)
    await running.load()

    await running.next()
    expect(request(fetchMock, 1).url).toBe('http://localhost:5173/api/auctions/running?page=1&size=2')
    expect(running.auctions.value.map((a) => a.id)).toEqual(['a3'])
    expect(running.hasNext.value).toBe(false)
    expect(running.hasPrev.value).toBe(true)

    await running.prev()
    expect(request(fetchMock, 2).url).toBe('http://localhost:5173/api/auctions/running?page=0&size=2')
  })

  it('shows an empty list when there is no running auction', async () => {
    fetchMock.mockResolvedValueOnce(halResponse(page('auctions', [], 0, 0)))
    const running = useRunningAuctions()

    await running.load()

    expect(running.auctions.value).toEqual([])
    expect(running.hasNext.value).toBe(false)
  })

  it('exposes the problem when the request fails', async () => {
    fetchMock.mockResolvedValueOnce(problemResponse({ title: 'Bad Request', status: 400, detail: 'size: must be ≤ 100' }))
    const running = useRunningAuctions()

    await running.load()

    expect(running.error.value?.status).toBe(400)
    expect(running.error.value?.message).toBe('size: must be ≤ 100')
    expect(running.loading.value).toBe(false)
  })
})
