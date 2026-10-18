import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { usePlaceBid } from '@/composables/usePlaceBid'
import { useAuctionStore } from '@/stores/auction'
import { auction, halResponse, page, problemResponse, request } from '@/testing/fixtures'

const fetchMock = vi.fn<typeof fetch>()

describe('usePlaceBid', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('posts the amount to the place-bid link, then reloads the auction', async () => {
    useAuctionStore().show(auction())
    fetchMock
      .mockResolvedValueOnce(halResponse({ id: 'b1', auctionId: 'a1', bidderId: 'u1', amount: 1250, _links: {} }, 201))
      .mockResolvedValueOnce(halResponse(auction({ currentHighestBid: 1250 })))
      .mockResolvedValueOnce(halResponse(page('bids', [], 0, 0)))
    const { placeBid, error } = usePlaceBid()

    await expect(placeBid(1250)).resolves.toBe(true)

    expect(request(fetchMock, 0)).toEqual({
      url: 'http://localhost:5173/api/auctions/a1/bids', method: 'POST', body: { amount: 1250 },
    })
    expect(request(fetchMock, 1).url).toBe('http://localhost:5173/api/auctions/a1')
    expect(useAuctionStore().current?.currentHighestBid).toBe(1250)
    expect(error.value).toBeNull()
  })

  it('shows why a bid was refused and reloads the auction to show the new state', async () => {
    useAuctionStore().show(auction())
    fetchMock
      .mockResolvedValueOnce(problemResponse({
        type: 'urn:bidanddrive:problem:bid-too-low', title: 'Bid too low', status: 409,
        detail: 'bid 1250.00 must be greater than 1300.00',
      }))
      .mockResolvedValueOnce(halResponse(auction({ currentHighestBid: 1300 })))
      .mockResolvedValueOnce(halResponse(page('bids', [], 0, 0)))
    const { placeBid, error, submitting } = usePlaceBid()

    await expect(placeBid(1250)).resolves.toBe(false)

    expect(error.value?.isType('bid-too-low')).toBe(true)
    expect(error.value?.message).toBe('bid 1250.00 must be greater than 1300.00')
    expect(useAuctionStore().current?.currentHighestBid).toBe(1300)
    expect(submitting.value).toBe(false)
  })

  it('does not call the API when the auction offers no place-bid link', async () => {
    useAuctionStore().show(auction({ status: 'ENDED', _links: { self: { href: '/api/auctions/a1' } } }))
    const { placeBid, error } = usePlaceBid()

    await expect(placeBid(2000)).resolves.toBe(false)

    expect(fetchMock).not.toHaveBeenCalled()
    expect(error.value?.problem.title).toBe('Bidding closed')
  })
})
