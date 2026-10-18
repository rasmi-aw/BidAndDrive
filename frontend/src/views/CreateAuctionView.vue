<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { CarCondition } from '@/api/types'
import ProblemAlert from '@/components/ProblemAlert.vue'
import { type AuctionForm, useCreateAuction } from '@/composables/useCreateAuction'
import { useAuctionStore } from '@/stores/auction'
import { toLocalInput } from '@/utils/format'

const conditions: CarCondition[] = ['EXCELLENT', 'GOOD', 'BAD']
const now = new Date()

const form = reactive<AuctionForm>({
  carName: '',
  condition: '',
  yearMade: null,
  yearRegistered: null,
  description: '',
  startPrice: null,
  startDate: toLocalInput(now),
  endDate: toLocalInput(new Date(now.getTime() + 7 * 24 * 3600 * 1000)),
})

const { create, submitting, error, fieldErrors } = useCreateAuction()
const auctionStore = useAuctionStore()
const router = useRouter()

async function submit(): Promise<void> {
  const created = await create(form)
  if (created) {
    auctionStore.show(created)
    await router.push({ name: 'auction', params: { id: created.id } })
  }
}
</script>

<template>
  <h1>Sell a car</h1>
  <form class="form" novalidate @submit.prevent="submit">
    <label>
      Car
      <input v-model="form.carName" maxlength="200" placeholder="Peugeot 308" />
      <small v-if="fieldErrors.carName" class="field-error">{{ fieldErrors.carName }}</small>
    </label>
    <label>
      Condition
      <select v-model="form.condition">
        <option value="" disabled>Choose…</option>
        <option v-for="c in conditions" :key="c" :value="c">{{ c.toLowerCase() }}</option>
      </select>
      <small v-if="fieldErrors.condition" class="field-error">{{ fieldErrors.condition }}</small>
    </label>
    <div class="row">
      <label>
        Year made
        <input v-model.number="form.yearMade" type="number" min="1886" />
        <small v-if="fieldErrors.yearMade" class="field-error">{{ fieldErrors.yearMade }}</small>
      </label>
      <label>
        Year registered
        <input v-model.number="form.yearRegistered" type="number" min="1886" />
        <small v-if="fieldErrors.yearRegistered" class="field-error">{{ fieldErrors.yearRegistered }}</small>
      </label>
    </div>
    <label>
      Description
      <textarea v-model="form.description" rows="4" maxlength="4000" />
      <small v-if="fieldErrors.description" class="field-error">{{ fieldErrors.description }}</small>
    </label>
    <label>
      Start price (EUR)
      <input v-model.number="form.startPrice" type="number" step="0.01" min="0.01" />
      <small v-if="fieldErrors.startPrice" class="field-error">{{ fieldErrors.startPrice }}</small>
    </label>
    <div class="row">
      <label>
        Starts
        <input v-model="form.startDate" type="datetime-local" />
        <small v-if="fieldErrors.startDate" class="field-error">{{ fieldErrors.startDate }}</small>
      </label>
      <label>
        Ends
        <input v-model="form.endDate" type="datetime-local" />
        <small v-if="fieldErrors.endDate" class="field-error">{{ fieldErrors.endDate }}</small>
      </label>
    </div>

    <!-- field errors are shown next to their inputs; this shows the problem title and rule violations -->
    <ProblemAlert :error="error" :show-field-errors="false" />
    <button type="submit" :disabled="submitting">{{ submitting ? 'Creating…' : 'Create auction' }}</button>
  </form>
</template>
