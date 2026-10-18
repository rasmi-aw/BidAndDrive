package com.auction.app.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapsKeycloakRealmRolesToSpringRoles() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("SELLER", "offline_access"))));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SELLER", "ROLE_offline_access");
    }

    @Test
    void tokenWithoutRealmRolesHasNoAuthorities() {
        assertThat(converter.convert(jwt(Map.of("scope", "openid")))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("realm_access", Map.of())))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("realm_access", "not-a-map")))).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token").header("alg", "none").subject("subject")
                .claims(c -> c.putAll(claims)).build();
    }
}
