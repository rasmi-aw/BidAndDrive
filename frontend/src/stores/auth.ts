import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { keycloak } from '@/auth/keycloak'

export type AuctionRole = 'SELLER' | 'BUYER'

export interface AuthUser {
  id: string
  name: string
  email?: string
}

/** The claims of a Keycloak access token this app uses. */
export interface TokenClaims {
  sub?: string
  name?: string
  preferred_username?: string
  email?: string
  realm_access?: { roles?: string[] }
}

/** Seconds of validity left below which the token is refreshed before a call. */
const MIN_TOKEN_VALIDITY = 30

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const roles = ref<string[]>([])
  /** false when Keycloak could not be reached at startup: the app still works anonymously. */
  const available = ref(true)

  const isAuthenticated = computed(() => user.value !== null)
  const isSeller = computed(() => roles.value.includes('SELLER'))
  const isBuyer = computed(() => roles.value.includes('BUYER'))

  function hasRole(role: AuctionRole): boolean {
    return roles.value.includes(role)
  }

  function setSession(claims: TokenClaims | undefined): void {
    if (!claims?.sub) {
      clearSession()
      return
    }
    user.value = { id: claims.sub, name: claims.name ?? claims.preferred_username ?? claims.sub, email: claims.email }
    roles.value = claims.realm_access?.roles ?? []
  }

  function clearSession(): void {
    user.value = null
    roles.value = []
  }

  function markUnavailable(): void {
    available.value = false
  }

  async function login(redirectPath = '/'): Promise<void> {
    await keycloak.login({ redirectUri: location.origin + redirectPath })
  }

  async function logout(): Promise<void> {
    await keycloak.logout({ redirectUri: `${location.origin}/` })
  }

  /** A valid access token for API calls, or undefined when anonymous or the session has ended. */
  async function accessToken(): Promise<string | undefined> {
    if (!keycloak.authenticated) {
      return undefined
    }
    try {
      await keycloak.updateToken(MIN_TOKEN_VALIDITY)
      return keycloak.token
    } catch {
      clearSession()
      return undefined
    }
  }

  return {
    user, roles, available, isAuthenticated, isSeller, isBuyer,
    hasRole, setSession, clearSession, markUnavailable, login, logout, accessToken,
  }
})
