package com.auction.app.web;

import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiTest extends ApiIntegrationTest {

    private static final Map<String, Object> PROFILE = Map.of(
            "name", "Kim Keycloak", "email", "Kim@Example.com", "phone_number", "+33 6 12 34 56 78");

    @Test
    void runningAuctionsAndApiDocsArePublic() throws Exception {
        mvc.perform(get("/api/auctions/running")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void everythingElseNeedsATokenAndAnswers401AsProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();
        for (var request : new RequestBuilder[]{
                get("/api/auctions/{id}", id),
                get("/api/auctions/{id}/bids", id),
                post("/api/auctions").contentType("application/json").content("{}"),
                post("/api/auctions/{id}/bids", id).contentType("application/json").content("{}")}) {
            mvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:unauthorized"));
        }
    }

    @Test
    void invalidTokenIsRejectedWith401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("signature does not match"));

        mvc.perform(get("/api/auctions/{id}", UUID.randomUUID()).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void buyerTokenCannotCreateAnAuction() throws Exception {
        postAuction(buyer(), auctionJson(now(), now().plus(Duration.ofDays(1)), "1000"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:forbidden"));
    }

    @Test
    void tokenWithoutAnAuctionRoleCanReadButNotWrite() throws Exception {
        var noRole = token(UUID.randomUUID(), PROFILE, "default-roles-bidanddrive", "offline_access");

        mvc.perform(get("/api/auctions/{id}", UUID.randomUUID()).with(noRole)).andExpect(status().isNotFound());
        mvc.perform(post("/api/auctions").with(noRole).contentType("application/json")
                        .content(auctionJson(now(), now().plus(Duration.ofDays(1)), "1000")))
                .andExpect(status().isForbidden());
    }

    @Test
    void firstRequestOfANewKeycloakUserProvisionsThemFromTheToken() throws Exception {
        UUID subject = UUID.randomUUID();

        mvc.perform(post("/api/auctions").with(token(subject, PROFILE, "SELLER")).contentType("application/json")
                        .content(auctionJson(now().minus(Duration.ofHours(1)), now().plus(Duration.ofDays(1)), "1000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerId").value(subject.toString()));

        User provisioned = userRepository.findById(new UserId(subject)).orElseThrow();
        assertThat(provisioned.getFullName()).isEqualTo("Kim Keycloak");
        assertThat(provisioned.getEmail().value()).isEqualTo("kim@example.com");
        assertThat(provisioned.getPhone().value()).isEqualTo("+33612345678");
        assertThat(provisioned.getRole()).isEqualTo(Role.SELLER);
    }

    @Test
    void roleChangedInKeycloakIsSyncedOnTheNextRequest() throws Exception {
        User formerSeller = seller();
        UUID auctionId = runningAuction(seller());

        mvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .with(token(formerSeller.getId().value(), PROFILE, "BUYER"))
                        .contentType("application/json").content("{\"amount\": 1500}"))
                .andExpect(status().isCreated());

        assertThat(userRepository.findById(formerSeller.getId()).orElseThrow().getRole()).isEqualTo(Role.BUYER);
    }

    @Test
    void tokenWithBothAuctionRolesIsAmbiguous() throws Exception {
        mvc.perform(post("/api/auctions").with(token(UUID.randomUUID(), PROFILE, "SELLER", "BUYER"))
                        .contentType("application/json")
                        .content(auctionJson(now(), now().plus(Duration.ofDays(1)), "1000")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:incomplete-identity"))
                .andExpect(jsonPath("$.detail", containsString("exactly one")));
    }

    @Test
    void tokenWithoutPhoneNumberCannotBeProvisioned() throws Exception {
        mvc.perform(post("/api/auctions")
                        .with(token(UUID.randomUUID(), Map.of("name", "No Phone", "email", "np@example.com"), "SELLER"))
                        .contentType("application/json")
                        .content(auctionJson(now(), now().plus(Duration.ofDays(1)), "1000")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:incomplete-identity"))
                .andExpect(jsonPath("$.detail", containsString("phone_number")));
    }
}
