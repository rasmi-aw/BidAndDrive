package com.auction.app.web;

import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreateAuctionApiTest extends ApiIntegrationTest {

    private static final String UUID_PATTERN = "[0-9a-f-]{36}";

    @Test
    void sellerCreatesARunningAuction() throws Exception {
        User seller = seller();

        postAuction(seller, auctionJson(now().minus(Duration.ofHours(1)), now().plus(Duration.ofDays(7)), "1000.50"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/auctions/" + UUID_PATTERN)))
                .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.sellerId").value(seller.getId().value().toString()))
                .andExpect(jsonPath("$.carName").value("Peugeot 308"))
                .andExpect(jsonPath("$.condition").value("GOOD"))
                .andExpect(jsonPath("$.yearMade").value(2018))
                .andExpect(jsonPath("$.yearRegistered").value(2019))
                .andExpect(jsonPath("$.description").value("One owner, full service history"))
                .andExpect(jsonPath("$.startPrice").value(1000.50))
                .andExpect(jsonPath("$.currentHighestBid").doesNotExist())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$._links.self.href", matchesPattern(".*/api/auctions/" + UUID_PATTERN)))
                .andExpect(jsonPath("$._links.bids.href", matchesPattern(".*/api/auctions/" + UUID_PATTERN + "/bids")))
                .andExpect(jsonPath("$._links.place-bid.href", endsWith("/bids")));
    }

    @Test
    void auctionStartingLaterHasNoPlaceBidLinkYet() throws Exception {
        postAuction(seller(), auctionJson(now().plus(Duration.ofDays(1)), now().plus(Duration.ofDays(7)), "1000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.place-bid").doesNotExist());
    }

    @Test
    void missingFieldsAreReportedAsValidationProblem() throws Exception {
        postAuction(seller(), "{\"carName\": \" \", \"startPrice\": -5}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:validation"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "carName", "condition", "yearMade", "yearRegistered", "startPrice", "startDate", "endDate")));
    }

    @Test
    void moreThanTwoDecimalsInStartPriceIsRejected() throws Exception {
        postAuction(seller(), auctionJson(now(), now().plus(Duration.ofDays(1)), "10.999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startPrice"));
    }

    @Test
    void domainRuleViolationIsABadRequest() throws Exception {
        postAuction(seller(), auctionJson(now().plus(Duration.ofDays(2)), now().plus(Duration.ofDays(1)), "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:invalid-value"))
                .andExpect(jsonPath("$.detail", containsString("endDate")));
    }

    @Test
    void unknownEnumValueOrMalformedJsonIsABadRequest() throws Exception {
        postAuction(seller(), auctionJson(now(), now().plus(Duration.ofDays(1)), "1000").replace("GOOD", "SHINY"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
        postAuction(seller(), "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }


    @Test
    void buyerIsForbiddenToCreateAnAuction() throws Exception {
        postAuction(buyer(), auctionJson(now(), now().plus(Duration.ofDays(1)), "1000"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:forbidden"));
    }
}
