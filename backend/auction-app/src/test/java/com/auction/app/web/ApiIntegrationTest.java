package com.auction.app.web;

import com.auction.app.web.security.KeycloakRealmRoleConverter;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import com.auction.domain.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application on H2 (Flyway schema). Every request commits for real; tables are cleared before each test.
 * Requests are authenticated with {@code jwt()} tokens shaped like Keycloak's, mapped by the real role converter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    protected static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
    private static final KeycloakRealmRoleConverter KEYCLOAK_ROLES = new KeycloakRealmRoleConverter();

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected Clock clock;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Only reached by requests with a raw Authorization header; jwt() bypasses the decoder. */
    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDatabase() {
        // test-only reset; production code never deletes rows
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    protected User seller() {
        return user(Role.SELLER);
    }

    protected User buyer() {
        return user(Role.BUYER);
    }

    /** Token of an existing user: subject = user id, Keycloak realm role = the user's role, profile claims. */
    protected static JwtRequestPostProcessor tokenOf(User user) {
        return token(user.getId().value(), Map.of(
                "name", user.getFullName(),
                "email", user.getEmail().value(),
                "phone_number", user.getPhone().value()), user.getRole().name());
    }

    /** Token for any authenticated caller who is never provisioned (read-only endpoints). */
    protected static JwtRequestPostProcessor anyToken() {
        return token(UUID.randomUUID(), Map.of(), "BUYER");
    }

    protected static JwtRequestPostProcessor token(UUID subject, Map<String, Object> profileClaims, String... realmRoles) {
        Map<String, Object> claims = new LinkedHashMap<>(profileClaims);
        claims.put("realm_access", Map.of("roles", List.of(realmRoles)));
        return jwt().jwt(jwt -> jwt.subject(subject.toString()).claims(c -> c.putAll(claims)))
                .authorities(KEYCLOAK_ROLES);
    }

    protected Instant now() {
        return clock.instant();
    }

    protected static String auctionJson(Instant startDate, Instant endDate, String startPrice) {
        return """
                {
                  "carName": "Peugeot 308",
                  "condition": "GOOD",
                  "yearMade": 2018,
                  "yearRegistered": 2019,
                  "description": "One owner, full service history",
                  "startPrice": %s,
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(startPrice, startDate, endDate);
    }

    protected ResultActions postAuction(User seller, String json) throws Exception {
        return mvc.perform(post("/api/auctions").with(tokenOf(seller))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Creates a running auction (opened an hour ago) through the API and returns its id. */
    protected UUID runningAuction(User seller, Duration endsIn) throws Exception {
        String body = postAuction(seller, auctionJson(now().minus(Duration.ofHours(1)), now().plus(endsIn), "1000"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    protected UUID runningAuction(User seller) throws Exception {
        return runningAuction(seller, Duration.ofDays(7));
    }

    protected ResultActions postBid(UUID auctionId, User bidder, String amount) throws Exception {
        return mvc.perform(post("/api/auctions/{id}/bids", auctionId).with(tokenOf(bidder))
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": " + amount + "}"));
    }

    private User user(Role role) {
        int n = SEQUENCE.incrementAndGet();
        return userRepository.save(User.register("User " + n, new Email("user" + n + "@example.com"),
                new Phone(String.format("+336%08d", n)), role, clock));
    }
}
