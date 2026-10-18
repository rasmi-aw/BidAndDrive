<script setup lang="ts">
import { computed } from 'vue'
import type { ApiError } from '@/api/http'

const props = withDefaults(defineProps<{ error: ApiError | null; showFieldErrors?: boolean }>(), {
  showFieldErrors: true,
})

const title = computed(() => {
  if (!props.error) return ''
  if (props.error.status === 401) return 'Please log in again'
  return props.error.problem.title ?? 'Something went wrong'
})

const detail = computed(() =>
  props.error?.status === 401 ? 'Your session has expired.' : props.error?.problem.detail,
)

const fieldErrors = computed(() => (props.showFieldErrors ? (props.error?.problem.errors ?? []) : []))
</script>

<template>
  <div v-if="error" class="problem" role="alert">
    <strong>{{ title }}</strong>
    <p v-if="detail">{{ detail }}</p>
    <ul v-if="fieldErrors.length">
      <li v-for="e in fieldErrors" :key="e.field"><code>{{ e.field }}</code>: {{ e.message }}</li>
    </ul>
  </div>
</template>
