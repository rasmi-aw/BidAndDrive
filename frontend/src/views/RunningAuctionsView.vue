<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Auction } from '@/api/types'
import ProblemAlert from '@/components/ProblemAlert.vue'
import { useRunningAuctions } from '@/composables/useRunningAuctions'
import { useAuctionStore } from '@/stores/auction'
import { formatDateTime, formatMoney } from '@/utils/format'

const running = useRunningAuctions(10)
const auctionStore = useAuctionStore()
const router = useRouter()

onMounted(() => running.load())

function open(auction: Auction): void {
  // hand the resource over; the detail page refreshes it through its self link
  auctionStore.show(auction)
  void router.push({ name: 'auction', params: { id: auction.id } })
}
</script>

<template>
  <h1>Running auctions</h1>
  <ProblemAlert :error="running.error.value" />
  <p v-if="running.loading.value && !running.page.value">Loading…</p>
  <p v-else-if="running.page.value && !running.auctions.value.length">No auction is running right now.</p>

  <ul class="cards">
    <li v-for="auction in running.auctions.value" :key="auction.id" class="card">
      <h2>{{ auction.carName }}</h2>
      <p class="muted">{{ auction.condition.toLowerCase() }} · made {{ auction.yearMade }} · registered {{ auction.yearRegistered }}</p>
      <p>
        <template v-if="auction.currentHighestBid !== undefined">
          Highest bid <strong>{{ formatMoney(auction.currentHighestBid) }}</strong>
        </template>
        <template v-else>Starting at <strong>{{ formatMoney(auction.startPrice) }}</strong></template>
      </p>
      <p class="muted">Ends {{ formatDateTime(auction.endDate) }}</p>
      <button @click="open(auction)">View auction</button>
    </li>
  </ul>

  <div v-if="running.page.value?.page.totalPages" class="pager">
    <button class="secondary" :disabled="!running.hasPrev.value || running.loading.value" @click="running.prev()">
      Previous
    </button>
    <span>Page {{ running.page.value.page.number + 1 }} of {{ running.page.value.page.totalPages }}</span>
    <button class="secondary" :disabled="!running.hasNext.value || running.loading.value" @click="running.next()">
      Next
    </button>
  </div>
</template>
