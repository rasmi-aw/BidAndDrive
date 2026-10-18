// Entry points only. Everything reachable from a resource is followed through its HAL links.

export const RUNNING_AUCTIONS = '/api/auctions/running'
export const AUCTIONS = '/api/auctions'

/** Only for deep links (page reload on /auctions/:id); navigation from a list uses the auction's self link. */
export function auctionUrl(id: string): string {
  return `${AUCTIONS}/${encodeURIComponent(id)}`
}
