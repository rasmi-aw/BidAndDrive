import { createRouter, createWebHistory } from 'vue-router'
import { type AuctionRole, useAuthStore } from '@/stores/auth'
import RunningAuctionsView from '@/views/RunningAuctionsView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresRole?: AuctionRole
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'running', component: RunningAuctionsView },
    {
      path: '/auctions/new',
      name: 'create-auction',
      component: () => import('@/views/CreateAuctionView.vue'),
      meta: { requiresAuth: true, requiresRole: 'SELLER' },
    },
    {
      path: '/auctions/:id',
      name: 'auction',
      component: () => import('@/views/AuctionDetailView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// UX only: the API enforces authentication and roles itself.
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    if (auth.available) {
      await auth.login(to.fullPath)
    }
    return false
  }
  if (to.meta.requiresRole && !auth.hasRole(to.meta.requiresRole)) {
    return { name: 'running' }
  }
  return true
})
