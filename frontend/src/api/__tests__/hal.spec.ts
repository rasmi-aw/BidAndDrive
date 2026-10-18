import { describe, expect, it } from 'vitest'
import { embedded, linkTo, requireLink } from '@/api/hal'
import type { HalResource, PagedResource } from '@/api/types'

const resource: HalResource = {
  _links: { self: { href: 'http://localhost:5173/api/auctions/1' }, 'place-bid': { href: '/api/auctions/1/bids' } },
}

describe('HAL helpers', () => {
  it('finds links by relation', () => {
    expect(linkTo(resource, 'self')).toBe('http://localhost:5173/api/auctions/1')
    expect(linkTo(resource, 'place-bid')).toBe('/api/auctions/1/bids')
    expect(linkTo(resource, 'next')).toBeUndefined()
    expect(linkTo(undefined, 'self')).toBeUndefined()
  })

  it('requireLink fails clearly when the server did not offer the action', () => {
    expect(() => requireLink(resource, 'bids')).toThrow("no 'bids' link")
  })

  it('reads embedded items and treats a missing _embedded as an empty page', () => {
    const page: PagedResource<{ id: string }> = {
      _embedded: { auctions: [{ id: '1' }] },
      _links: {},
      page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
    }
    const empty: PagedResource<{ id: string }> = {
      _links: {},
      page: { size: 10, number: 0, totalElements: 0, totalPages: 0 },
    }

    expect(embedded(page, 'auctions')).toEqual([{ id: '1' }])
    expect(embedded(empty, 'auctions')).toEqual([])
  })
})
