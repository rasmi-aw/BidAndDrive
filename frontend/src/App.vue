<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <header class="topbar">
    <RouterLink to="/" class="brand">BidAndDrive</RouterLink>
    <nav>
      <RouterLink to="/">Running auctions</RouterLink>
      <RouterLink v-if="auth.isSeller" :to="{ name: 'create-auction' }">Sell a car</RouterLink>
    </nav>
    <div class="session">
      <template v-if="auth.isAuthenticated">
        <span>{{ auth.user?.name }}</span>
        <span v-if="auth.isSeller" class="badge">Seller</span>
        <span v-if="auth.isBuyer" class="badge">Buyer</span>
        <button class="secondary" @click="auth.logout()">Log out</button>
      </template>
      <button v-else :disabled="!auth.available" @click="auth.login($route.fullPath)">Log in</button>
    </div>
  </header>
  <p v-if="!auth.available" class="notice" role="status">
    Login is unavailable right now (Keycloak not reachable). You can still browse running auctions.
  </p>
  <main>
    <RouterView />
  </main>
</template>
