import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/http'
import { useAuctionStore } from '@/stores/auction'
import { auction, halResponse, page, problemResponse, request } from '@/testing/fixtures'

const fetchMock = vi.fn<typeof fetch>()

describe('auction store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('shows an auction from a list immediately, without a request', () => {
    const store = useAuctionStore()

    store.show(auction())

    expect(store.current?.id).toBe('a1')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('loads an auction, then its bids by following the bids link', async () => {
    const bid = { id: 'b1', auctionId: 'a1', bidderId: 'u1', amount: 1200, createdAt: '2026-10-18T10:00:00Z', _links: {} }
    fetchMock
      .mockResolvedValueOnce(halResponse(auction({ currentHighestBid: 1200 })))
      .mockResolvedValueOnce(halResponse(page('bids', [bid], 0, 1)))
    const store = useAuctionStore()

    await store.load('/api/auctions/a1')

    expect(request(fetchMock, 0).url).toBe('/api/auctions/a1')
    expect(request(fetchMock, 1).url).toBe('http://localhost:5173/api/auctions/a1/bids')
    expect(store.current?.currentHighestBid).toBe(1200)
    expect(store.bids).toEqual([bid])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('refreshes through the self link', async () => {
    fetchMock.mockResolvedValueOnce(halResponse(auction())).mockResolvedValueOnce(halResponse(page('bids', [], 0, 0)))
    const store = useAuctionStore()
    store.show(auction())

    await store.refresh()

    expect(request(fetchMock, 0).url).toBe('http://localhost:5173/api/auctions/a1')
    expect(store.bids).toEqual([])
  })

  it('offers bidding only while the server sends a place-bid link', () => {
    const store = useAuctionStore()

    store.show(auction())
    expect(store.canBid).toBe(true)

    store.show(auction({ status: 'ENDED', _links: { self: { href: '/api/auctions/a1' } } }))
    expect(store.canBid).toBe(false)
  })

  it('computes the minimum bid from the start price and the highest bid', () => {
    const store = useAuctionStore()

    store.show(auction({ startPrice: 1000 }))
    expect(store.minimumBid).toBe(1000)

    store.show(auction({ startPrice: 1000, currentHighestBid: 1500 }))
    expect(store.minimumBid).toBe(1500)
  })

  it('keeps the backend problem when loading fails', async () => {
    fetchMock.mockResolvedValueOnce(problemResponse({
      type: 'urn:bidanddrive:problem:not-found', title: 'Resource not found', status: 404, detail: 'auction x not found',
    }))
    const store = useAuctionStore()

    await store.load('/api/auctions/x')

    expect(store.error).toBeInstanceOf(ApiError)
    expect(store.error?.status).toBe(404)
    expect(store.error?.isType('not-found')).toBe(true)
    expect(store.current).toBeNull()
    expect(store.loading).toBe(false)
  })
})
