<script setup lang="ts">
import { ref, watch } from 'vue'
import { auctionUrl } from '@/api/endpoints'
import ProblemAlert from '@/components/ProblemAlert.vue'
import { usePlaceBid } from '@/composables/usePlaceBid'
import { useAuctionStore } from '@/stores/auction'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, formatMoney } from '@/utils/format'

const props = defineProps<{ id: string }>()

const store = useAuctionStore()
const auth = useAuthStore()
const { placeBid, submitting, error: bidError } = usePlaceBid()

const amount = ref<number | null>(null)
const accepted = ref(false)

watch(
  () => props.id,
  async (id) => {
    accepted.value = false
    if (store.current?.id === id) {
      await store.refresh()
    } else {
      // deep link: the only place the auction URL is built from its id
      store.clear()
      await store.load(auctionUrl(id))
    }
  },
  { immediate: true },
)

async function submitBid(): Promise<void> {
  accepted.value = false
  if (amount.value !== null && (await placeBid(amount.value))) {
    accepted.value = true
    amount.value = null
  }
}
</script>

<template>
  <RouterLink to="/" class="back">← Running auctions</RouterLink>
  <ProblemAlert :error="store.error" />
  <p v-if="store.loading && !store.current">Loading…</p>

  <article v-if="store.current" class="detail">
    <header>
      <h1>{{ store.current.carName }}</h1>
      <span class="badge" :class="store.current.status.toLowerCase()">{{ store.current.status }}</span>
    </header>
    <p class="muted">
      {{ store.current.condition.toLowerCase() }} condition · made {{ store.current.yearMade }} · registered
      {{ store.current.yearRegistered }}
    </p>
    <p v-if="store.current.description">{{ store.current.description }}</p>

    <dl>
      <dt>Start price</dt>
      <dd>{{ formatMoney(store.current.startPrice) }}</dd>
      <dt>Highest bid</dt>
      <dd>{{ store.current.currentHighestBid !== undefined ? formatMoney(store.current.currentHighestBid) : 'No bid yet' }}</dd>
      <dt>Starts</dt>
      <dd>{{ formatDateTime(store.current.startDate) }}</dd>
      <dt>Ends</dt>
      <dd>{{ formatDateTime(store.current.endDate) }}</dd>
    </dl>

    <section class="bid">
      <h2>Place a bid</h2>
      <template v-if="!store.canBid">
        <p class="muted">This auction is not open for bidding.</p>
      </template>
      <template v-else-if="!auth.isBuyer">
        <p class="muted">Only buyers can bid.</p>
      </template>
      <form v-else @submit.prevent="submitBid">
        <label>
          Your bid (more than {{ formatMoney(store.minimumBid) }})
          <input v-model.number="amount" type="number" step="0.01" :min="store.minimumBid + 0.01" required />
        </label>
        <button type="submit" :disabled="submitting">{{ submitting ? 'Placing…' : 'Place bid' }}</button>
      </form>
      <p v-if="accepted" class="success" role="status">Your bid was accepted.</p>
      <ProblemAlert :error="bidError" />
    </section>

    <section>
      <h2>Bids</h2>
      <p v-if="!store.bids.length" class="muted">No bid yet.</p>
      <table v-else>
        <thead>
          <tr><th>Amount</th><th>Placed</th></tr>
        </thead>
        <tbody>
          <tr v-for="bid in store.bids" :key="bid.id">
            <td>{{ formatMoney(bid.amount) }}</td>
            <td>{{ bid.createdAt ? formatDateTime(bid.createdAt) : '' }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </article>
</template>
