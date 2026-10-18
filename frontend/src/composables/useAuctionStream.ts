import { ref } from 'vue'
import { linkTo } from '@/api/hal'
import { ApiError } from '@/api/http'
import { type SseMessage, openEventStream } from '@/api/sse'
import { type BidEvent, Rel } from '@/api/types'
import { useAuctionStore } from '@/stores/auction'

/**
 * Live updates of the current auction through its `stream` link: new bids update the store as they happen,
 * the closing reloads the auction. Reconnects when the stream drops and reloads to catch up on missed bids.
 */
export function useAuctionStream({ retryDelayMs = 3000 } = {}) {
  const store = useAuctionStore()
  const connected = ref(false)
  let controller: AbortController | null = null

  function start(): void {
    stop()
    const href = linkTo(store.current, Rel.STREAM)
    if (!href) return
    controller = new AbortController()
    void run(href, controller.signal)
  }

  function stop(): void {
    controller?.abort()
    controller = null
    connected.value = false
  }

  async function run(href: string, signal: AbortSignal): Promise<void> {
    let reconnecting = false
    let closed = false
    const onEvent = (message: SseMessage): void => {
      if (message.event === 'connected') {
        connected.value = true
        if (reconnecting) void store.refresh() // bids placed while disconnected
      } else if (message.event === 'bid') {
        store.applyBid(JSON.parse(message.data) as BidEvent)
      } else if (message.event === 'closed') {
        closed = true
        void store.refresh() // status, winner, no more place-bid link
      }
    }
    while (!signal.aborted && !closed) {
      try {
        await openEventStream(href, { onEvent, signal })
      } catch (error) {
        // 401 / 403 / 404: reconnecting will not help
        if (signal.aborted || (error instanceof ApiError && error.status >= 400 && error.status < 500)) break
      }
      connected.value = false
      reconnecting = true
      if (!closed && !signal.aborted) await new Promise((resolve) => setTimeout(resolve, retryDelayMs))
    }
    connected.value = false
  }

  return { connected, start, stop }
}
