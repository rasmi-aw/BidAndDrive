import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { embedded, linkTo } from '@/api/hal'
import { type ApiError, apiFetch, toApiError } from '@/api/http'
import { type Auction, type Bid, type PagedResource, Rel } from '@/api/types'

/** The auction being looked at, shared by the detail page and the bid form. */
export const useAuctionStore = defineStore('auction', () => {
  const current = ref<Auction | null>(null)
  const bids = ref<Bid[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)

  /** The server decides: bidding is offered only while the auction carries a place-bid link. */
  const canBid = computed(() => linkTo(current.value, Rel.PLACE_BID) !== undefined)

  /** A bid must be strictly greater than this amount. */
  const minimumBid = computed(() =>
    current.value ? Math.max(current.value.startPrice, current.value.currentHighestBid ?? 0) : 0,
  )

  /** Shows an auction already in hand (from a list or a create response) before it is refreshed. */
  function show(auction: Auction): void {
    if (current.value?.id !== auction.id) {
      bids.value = []
    }
    current.value = auction
    error.value = null
  }

  async function load(href: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      current.value = await apiFetch<Auction>(href)
      await loadBids()
    } catch (e) {
      error.value = toApiError(e)
    } finally {
      loading.value = false
    }
  }

  async function refresh(): Promise<void> {
    const self = linkTo(current.value, Rel.SELF)
    if (self) {
      await load(self)
    }
  }

  async function loadBids(): Promise<void> {
    const href = linkTo(current.value, Rel.BIDS)
    bids.value = href ? embedded(await apiFetch<PagedResource<Bid>>(href), 'bids') : []
  }

  function clear(): void {
    current.value = null
    bids.value = []
    error.value = null
  }

  return { current, bids, loading, error, canBid, minimumBid, show, load, refresh, clear }
})
