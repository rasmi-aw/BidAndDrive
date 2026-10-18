// Shapes of the backend's HAL resources and RFC 9457 problem details.

export interface Link {
  href: string
}

export type Links = Partial<Record<string, Link>>

export interface HalResource {
  _links: Links
}

export const Rel = {
  SELF: 'self',
  NEXT: 'next',
  PREV: 'prev',
  BIDS: 'bids',
  PLACE_BID: 'place-bid',
  AUCTION: 'auction',
} as const

export type AuctionStatus = 'RUNNING' | 'CANCELLED' | 'ENDED'
export type CarCondition = 'BAD' | 'GOOD' | 'EXCELLENT'

export interface Auction extends HalResource {
  id: string
  sellerId: string
  carName: string
  condition: CarCondition
  yearMade: number
  yearRegistered: number
  description?: string
  startPrice: number
  currentHighestBid?: number
  startDate: string
  endDate: string
  status: AuctionStatus
}

export interface Bid extends HalResource {
  id: string
  auctionId: string
  bidderId: string
  amount: number
  /** absent in the response to placing a bid */
  createdAt?: string
}

export interface PageMetadata {
  size: number
  number: number
  totalElements: number
  totalPages: number
}

export interface PagedResource<T> extends HalResource {
  /** absent when the page is empty (HAL) */
  _embedded?: Partial<Record<string, T[]>>
  page: PageMetadata
}

export interface FieldError {
  field: string
  message: string
}

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}

export interface NewAuction {
  carName: string
  condition: CarCondition
  yearMade: number
  yearRegistered: number
  description?: string
  startPrice: number
  startDate: string
  endDate: string
}
