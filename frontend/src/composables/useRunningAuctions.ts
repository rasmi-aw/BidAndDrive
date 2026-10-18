import { computed, ref } from 'vue'
import { RUNNING_AUCTIONS } from '@/api/endpoints'
import { embedded, linkTo, requireLink } from '@/api/hal'
import { type ApiError, apiFetch, toApiError } from '@/api/http'
import { type Auction, type PagedResource, Rel } from '@/api/types'

/** Public list of auctions open for bidding; pages by following the server's next/prev links. */
export function useRunningAuctions(pageSize = 10) {
  const page = ref<PagedResource<Auction> | null>(null)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)

  const auctions = computed(() => (page.value ? embedded(page.value, 'auctions') : []))
  const hasNext = computed(() => linkTo(page.value, Rel.NEXT) !== undefined)
  const hasPrev = computed(() => linkTo(page.value, Rel.PREV) !== undefined)

  async function load(href = `${RUNNING_AUCTIONS}?page=0&size=${pageSize}`): Promise<void> {
    loading.value = true
    error.value = null
    try {
      page.value = await apiFetch<PagedResource<Auction>>(href)
    } catch (e) {
      error.value = toApiError(e)
    } finally {
      loading.value = false
    }
  }

  const next = () => load(requireLink(page.value, Rel.NEXT))
  const prev = () => load(requireLink(page.value, Rel.PREV))

  return { page, auctions, loading, error, hasNext, hasPrev, load, next, prev }
}
