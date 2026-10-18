import { ref } from 'vue'
import { linkTo } from '@/api/hal'
import { ApiError, apiFetch, toApiError } from '@/api/http'
import { type Bid, Rel } from '@/api/types'
import { useAuctionStore } from '@/stores/auction'

/** Places a bid on the current auction through its place-bid link. */
export function usePlaceBid() {
  const auctionStore = useAuctionStore()
  const submitting = ref(false)
  const error = ref<ApiError | null>(null)

  async function placeBid(amount: number): Promise<boolean> {
    const href = linkTo(auctionStore.current, Rel.PLACE_BID)
    if (!href) {
      error.value = new ApiError(0, { title: 'Bidding closed', detail: 'This auction no longer accepts bids.' })
      return false
    }
    submitting.value = true
    error.value = null
    try {
      await apiFetch<Bid>(href, { method: 'POST', body: { amount } })
      await auctionStore.refresh()
      return true
    } catch (e) {
      error.value = toApiError(e)
      if (error.value.status === 409) {
        // outbid, closed or a concurrent bid won: show the auction as it is now
        await auctionStore.refresh()
      }
      return false
    } finally {
      submitting.value = false
    }
  }

  return { placeBid, submitting, error }
}
