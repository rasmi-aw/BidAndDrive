import { computed, ref } from 'vue'
import { AUCTIONS } from '@/api/endpoints'
import { ApiError, apiFetch, toApiError } from '@/api/http'
import type { Auction, CarCondition, FieldError, NewAuction } from '@/api/types'

/** Form model: numbers may be empty, dates come from datetime-local inputs (user's time zone). */
export interface AuctionForm {
  carName: string
  condition: CarCondition | ''
  yearMade: number | null
  yearRegistered: number | null
  description: string
  startPrice: number | null
  startDate: string
  endDate: string
}

/** Creates an auction (SELLER only). Validation happens on the server; its field errors are exposed per field. */
export function useCreateAuction() {
  const submitting = ref(false)
  const error = ref<ApiError | null>(null)
  const fieldErrors = computed(() => error.value?.fieldErrors ?? {})

  async function create(form: AuctionForm): Promise<Auction | undefined> {
    const localErrors = validate(form)
    if (localErrors.length) {
      error.value = new ApiError(400, {
        title: 'Validation failed',
        detail: 'Please correct the highlighted fields.',
        errors: localErrors,
      })
      return undefined
    }
    submitting.value = true
    error.value = null
    try {
      return await apiFetch<Auction>(AUCTIONS, { method: 'POST', body: toPayload(form) })
    } catch (e) {
      error.value = toApiError(e)
      return undefined
    } finally {
      submitting.value = false
    }
  }

  return { create, submitting, error, fieldErrors }
}

/**
 * Cross-field rules checked before calling the API, for immediate feedback next to the field.
 * The server checks the same rules; missing values are left to it.
 */
function validate(form: AuctionForm): FieldError[] {
  const errors: FieldError[] = []
  if (form.yearMade !== null && form.yearRegistered !== null && form.yearRegistered < form.yearMade) {
    errors.push({ field: 'yearRegistered', message: `must not be before the year made (${form.yearMade})` })
  }
  return errors
}

/** Empty values are sent as null so the server reports them as missing fields. */
function toPayload(form: AuctionForm): Partial<Record<keyof NewAuction, unknown>> {
  const description = form.description.trim()
  return {
    carName: form.carName.trim(),
    condition: form.condition || null,
    yearMade: form.yearMade,
    yearRegistered: form.yearRegistered,
    ...(description ? { description } : {}),
    startPrice: form.startPrice,
    startDate: toInstant(form.startDate),
    endDate: toInstant(form.endDate),
  }
}

function toInstant(localDateTime: string): string | null {
  const date = new Date(localDateTime)
  return localDateTime && !Number.isNaN(date.getTime()) ? date.toISOString() : null
}
