// Test data shaped like the backend's HAL / ProblemDetail responses. Only imported by tests.
import type { Auction, PagedResource, ProblemDetail } from '@/api/types'

const BASE = 'http://localhost:5173/api/auctions'

export function auction(overrides: Partial<Auction> = {}): Auction {
  const id = overrides.id ?? 'a1'
  return {
    id,
    sellerId: 'seller-1',
    carName: 'Peugeot 308',
    condition: 'GOOD',
    yearMade: 2018,
    yearRegistered: 2019,
    description: 'One owner',
    startPrice: 1000,
    startDate: '2026-10-17T10:00:00Z',
    endDate: '2026-10-25T10:00:00Z',
    status: 'RUNNING',
    _links: {
      self: { href: `${BASE}/${id}` },
      bids: { href: `${BASE}/${id}/bids` },
      'place-bid': { href: `${BASE}/${id}/bids` },
    },
    ...overrides,
  }
}

export function page<T>(rel: string, items: T[], number: number, totalPages: number): PagedResource<T> {
  const links: PagedResource<T>['_links'] = { self: { href: `${BASE}/running?page=${number}&size=2` } }
  if (number > 0) links.prev = { href: `${BASE}/running?page=${number - 1}&size=2` }
  if (number + 1 < totalPages) links.next = { href: `${BASE}/running?page=${number + 1}&size=2` }
  return {
    ...(items.length ? { _embedded: { [rel]: items } } : {}),
    _links: links,
    page: { size: 2, number, totalElements: totalPages * 2, totalPages },
  }
}

export function halResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/hal+json' } })
}

export function problemResponse(problem: ProblemDetail): Response {
  return new Response(JSON.stringify(problem), {
    status: problem.status ?? 400,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

/** URL and parsed JSON body of the n-th fetch call. */
export function request(fetchMock: { mock: { calls: unknown[][] } }, n: number): { url: string; method: string; body: unknown } {
  const [url, init] = fetchMock.mock.calls[n] as [string, RequestInit | undefined]
  return {
    url,
    method: init?.method ?? 'GET',
    body: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
  }
}
