import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { keycloak } from '@/auth/keycloak'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/auth/keycloak', () => ({
  keycloak: {
    authenticated: false,
    token: undefined as string | undefined,
    updateToken: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

const sellerClaims = {
  sub: 'b52f6f59-9e87-47b2-96d7-7993cb921ea9',
  name: 'Sam Seller',
  preferred_username: 'seller',
  email: 'seller@bidanddrive.local',
  realm_access: { roles: ['offline_access', 'default-roles-bidanddrive', 'SELLER'] },
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('location', { origin: 'http://localhost:5173' })
    keycloak.authenticated = false
    keycloak.token = undefined
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.unstubAllGlobals()
  })

  it('starts anonymous', () => {
    const auth = useAuthStore()

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBeNull()
    expect(auth.isSeller).toBe(false)
    expect(auth.isBuyer).toBe(false)
  })

  it('takes the user and the realm roles from the token claims', () => {
    const auth = useAuthStore()

    auth.setSession(sellerClaims)

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.user).toEqual({ id: sellerClaims.sub, name: 'Sam Seller', email: 'seller@bidanddrive.local' })
    expect(auth.isSeller).toBe(true)
    expect(auth.isBuyer).toBe(false)
    expect(auth.hasRole('SELLER')).toBe(true)
  })

  it('falls back to the username when the token has no name', () => {
    const auth = useAuthStore()

    auth.setSession({ ...sellerClaims, name: undefined })

    expect(auth.user?.name).toBe('seller')
  })

  it('treats missing claims as logged out and can clear the session', () => {
    const auth = useAuthStore()
    auth.setSession(sellerClaims)

    auth.setSession(undefined)
    expect(auth.isAuthenticated).toBe(false)

    auth.setSession(sellerClaims)
    auth.clearSession()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.roles).toEqual([])
  })

  it('gives no token to anonymous calls', async () => {
    await expect(useAuthStore().accessToken()).resolves.toBeUndefined()
    expect(keycloak.updateToken).not.toHaveBeenCalled()
  })

  it('refreshes the token if it expires within 30 seconds before handing it out', async () => {
    keycloak.authenticated = true
    keycloak.token = 'fresh-token'
    vi.mocked(keycloak.updateToken).mockResolvedValue(true)

    await expect(useAuthStore().accessToken()).resolves.toBe('fresh-token')
    expect(keycloak.updateToken).toHaveBeenCalledWith(30)
  })

  it('ends the session when the token can no longer be refreshed', async () => {
    const auth = useAuthStore()
    auth.setSession(sellerClaims)
    keycloak.authenticated = true
    vi.mocked(keycloak.updateToken).mockRejectedValue(new Error('session expired'))

    await expect(auth.accessToken()).resolves.toBeUndefined()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('logs in through Keycloak and comes back to the requested page', async () => {
    await useAuthStore().login('/auctions/new')

    expect(keycloak.login).toHaveBeenCalledWith({ redirectUri: 'http://localhost:5173/auctions/new' })
  })
})
