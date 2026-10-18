import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { type AuctionForm, useCreateAuction } from '@/composables/useCreateAuction'
import { auction, halResponse, problemResponse, request } from '@/testing/fixtures'

const fetchMock = vi.fn<typeof fetch>()

function form(overrides: Partial<AuctionForm> = {}): AuctionForm {
  return {
    carName: 'Renault Clio',
    condition: 'EXCELLENT',
    yearMade: 2021,
    yearRegistered: 2022,
    description: '  ',
    startPrice: 5000,
    startDate: '2026-11-01T09:00',
    endDate: '2026-11-08T18:30',
    ...overrides,
  }
}

describe('useCreateAuction', () => {
  beforeEach(() => vi.stubGlobal('fetch', fetchMock))

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('posts the form as an API payload and returns the created auction', async () => {
    fetchMock.mockResolvedValueOnce(halResponse(auction({ id: 'new-1' }), 201))
    const { create } = useCreateAuction()

    const created = await create(form())

    expect(created?.id).toBe('new-1')
    expect(request(fetchMock, 0)).toEqual({
      url: '/api/auctions',
      method: 'POST',
      body: {
        carName: 'Renault Clio',
        condition: 'EXCELLENT',
        yearMade: 2021,
        yearRegistered: 2022,
        startPrice: 5000,
        // datetime-local inputs are in the user's time zone; the API takes instants
        startDate: new Date('2026-11-01T09:00').toISOString(),
        endDate: new Date('2026-11-08T18:30').toISOString(),
      },
    })
  })

  it('maps validation problems to form fields', async () => {
    fetchMock.mockResolvedValueOnce(problemResponse({
      type: 'urn:bidanddrive:problem:validation', title: 'Validation failed', status: 400,
      errors: [{ field: 'carName', message: 'must not be blank' }, { field: 'startDate', message: 'must not be null' }],
    }))
    const { create, fieldErrors, error } = useCreateAuction()

    await expect(create(form({ carName: '', startDate: '' }))).resolves.toBeUndefined()

    expect(request(fetchMock, 0).body).toMatchObject({ carName: '', startDate: null })
    expect(fieldErrors.value).toEqual({ carName: 'must not be blank', startDate: 'must not be null' })
    expect(error.value?.status).toBe(400)
  })

  it('surfaces business rule problems that are not tied to a field', async () => {
    fetchMock.mockResolvedValueOnce(problemResponse({
      type: 'urn:bidanddrive:problem:invalid-value', title: 'Invalid value', status: 400,
      detail: 'endDate (2026-11-01T08:00:00Z) must be after startDate (2026-11-08T17:30:00Z)',
    }))
    const { create, fieldErrors, error } = useCreateAuction()

    await create(form({ startDate: '2026-11-08T18:30', endDate: '2026-11-01T09:00' }))

    expect(fieldErrors.value).toEqual({})
    expect(error.value?.message).toContain('must be after startDate')
  })
})
