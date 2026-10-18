package com.auction.app.web;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application on H2 (Flyway schema). Every request commits for real; tables are cleared before each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    protected static final String USER_HEADER = "X-User-Id";
    protected static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected Clock clock;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return mvc.perform(post("/api/auctions").header(USER_HEADER, seller.getId().value())
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
        return mvc.perform(post("/api/auctions/{id}/bids", auctionId).header(USER_HEADER, bidder.getId().value())
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": " + amount + "}"));
    }

    private User user(Role role) {
        int n = SEQUENCE.incrementAndGet();
        return userRepository.save(User.register("User " + n, new Email("user" + n + "@example.com"),
                new Phone(String.format("+336%08d", n)), role, clock));
    }
}
