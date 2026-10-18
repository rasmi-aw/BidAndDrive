import Keycloak from 'keycloak-js'

// Realm and client come from docker/keycloak/bidanddrive-realm.json (public client with PKCE).
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'bidanddrive',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'auction-frontend',
})

/** Restores an existing Keycloak session without forcing a login (the running list is public). */
export function initKeycloak(): Promise<boolean> {
  return keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    checkLoginIframe: false,
  })
}
