import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setTokenProvider } from '@/api/http'
import { useAuctionStream } from '@/composables/useAuctionStream'
import { useAuctionStore } from '@/stores/auction'
import { auction, halResponse, page, request } from '@/testing/fixtures'

const fetchMock = vi.fn<typeof fetch>()
const encoder = new TextEncoder()
const STREAM = 'http://localhost:5173/api/auctions/a1/stream'
const withStream = () => auction({ _links: { ...auction()._links, stream: { href: STREAM } } })

/** A stream that stays open until the test closes it. */
function openStream() {
  let controller!: ReadableStreamDefaultController<Uint8Array>
  const response = new Response(new ReadableStream<Uint8Array>({ start: (c) => { controller = c } }),
    { headers: { 'Content-Type': 'text/event-stream' } })
  return {
    response,
    send: (event: string, data: unknown) => controller.enqueue(encoder.encode(`event:${event}\ndata:${JSON.stringify(data)}\n\n`)),
    end: () => controller.close(),
  }
}

const tick = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('useAuctionStream', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', fetchMock)
    setTokenProvider(async () => 'token-123')
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('follows the stream link and applies live bids to the auction', async () => {
    const store = useAuctionStore()
    store.show(withStream())
    const stream = openStream()
    fetchMock.mockResolvedValueOnce(stream.response)
    const live = useAuctionStream({ retryDelayMs: 0 })

    live.start()
    stream.send('connected', { auctionId: 'a1' })
    await vi.waitFor(() => expect(live.connected.value).toBe(true))
    stream.send('bid', { auctionId: 'a1', bidId: 'b1', bidderId: 'u1', amount: 1400, occurredAt: '2026-10-18T10:00:00Z' })

    await vi.waitFor(() => expect(store.current?.currentHighestBid).toBe(1400))
    expect(request(fetchMock, 0).url).toBe(STREAM)
    live.stop()
    expect(live.connected.value).toBe(false)
  })

  it('reloads the auction when it closes and does not reconnect', async () => {
    const store = useAuctionStore()
    store.show(withStream())
    const stream = openStream()
    fetchMock
      .mockResolvedValueOnce(stream.response)
      .mockResolvedValueOnce(halResponse(auction({
        status: 'ENDED',
        _links: { self: { href: '/api/auctions/a1' }, bids: { href: '/api/auctions/a1/bids' } },
      })))
      .mockResolvedValueOnce(halResponse(page('bids', [], 0, 0)))
    const live = useAuctionStream({ retryDelayMs: 0 })

    live.start()
    stream.send('closed', { auctionId: 'a1', occurredAt: '2026-10-18T10:00:00Z' })
    stream.end()

    await vi.waitFor(() => expect(store.current?.status).toBe('ENDED'))
    await tick()
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(live.connected.value).toBe(false)
  })

  it('reconnects when the stream drops and catches up on what it missed', async () => {
    const store = useAuctionStore()
    store.show(withStream())
    const first = openStream()
    const second = openStream()
    fetchMock
      .mockResolvedValueOnce(first.response)
      .mockResolvedValueOnce(second.response)
      .mockResolvedValueOnce(halResponse(withStream()))
      .mockResolvedValueOnce(halResponse(page('bids', [], 0, 0)))
    const live = useAuctionStream({ retryDelayMs: 0 })

    live.start()
    first.send('connected', {})
    first.end()
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    second.send('connected', {})

    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(4))
    expect(request(fetchMock, 1).url).toBe(STREAM)
    expect(request(fetchMock, 2).url).toBe('http://localhost:5173/api/auctions/a1')
    live.stop()
  })

  it('does nothing when the auction offers no stream link', () => {
    useAuctionStore().show(auction())

    useAuctionStream().start()

    expect(fetchMock).not.toHaveBeenCalled()
  })
})
