package com.auction.app.web;

import com.auction.application.command.CloseAuctionCommand;
import com.auction.application.command.CloseAuctionUseCase;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BidsApiTest extends ApiIntegrationTest {

    @Autowired
    private CloseAuctionUseCase closeAuction;

    @Test
    void buyerPlacesABidAndTheAuctionShowsTheNewHighestBid() throws Exception {
        UUID auctionId = runningAuction(seller());
        User buyer = buyer();

        postBid(auctionId, buyer, "1200.50")
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.auctionId").value(auctionId.toString()))
                .andExpect(jsonPath("$.bidderId").value(buyer.getId().value().toString()))
                .andExpect(jsonPath("$.amount").value(1200.50))
                .andExpect(jsonPath("$._links.auction.href", endsWith("/api/auctions/" + auctionId)))
                .andExpect(jsonPath("$._links.bids.href", endsWith("/api/auctions/" + auctionId + "/bids")));

        mvc.perform(get("/api/auctions/{id}", auctionId))
                .andExpect(jsonPath("$.currentHighestBid").value(1200.50));
    }

    @Test
    void listsBidsNewestFirst() throws Exception {
        UUID auctionId = runningAuction(seller());
        postBid(auctionId, buyer(), "1100").andExpect(status().isCreated());
        postBid(auctionId, buyer(), "1200").andExpect(status().isCreated());

        mvc.perform(get("/api/auctions/{id}/bids", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.bids[*].amount", contains(1200.0, 1100.0)))
                .andExpect(jsonPath("$._embedded.bids[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$._links.self.href", containsString("/bids")));
    }

    @Test
    void listingBidsOfUnknownAuctionIsNotFound() throws Exception {
        mvc.perform(get("/api/auctions/{id}/bids", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidAmountIsAValidationProblem() throws Exception {
        UUID auctionId = runningAuction(seller());

        postBid(auctionId, buyer(), "null")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
        postBid(auctionId, buyer(), "-1").andExpect(status().isBadRequest());
        postBid(auctionId, buyer(), "1200.001").andExpect(status().isBadRequest());
    }

    @Test
    void sellerIsForbiddenToBidOnTheirOwnAuction() throws Exception {
        User seller = seller();
        UUID auctionId = runningAuction(seller);

        postBid(auctionId, seller, "2000")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:forbidden"))
                .andExpect(jsonPath("$.detail", containsString("own auction")));
    }

    @Test
    void anotherSellerIsForbiddenToBid() throws Exception {
        UUID auctionId = runningAuction(seller());

        postBid(auctionId, seller(), "2000")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("BUYER")));
    }

    @Test
    void unknownAuctionOrBidderIsNotFound() throws Exception {
        postBid(UUID.randomUUID(), buyer(), "2000").andExpect(status().isNotFound());

        UUID auctionId = runningAuction(seller());
        mvc.perform(post("/api/auctions/{id}/bids", auctionId).header(USER_HEADER, UUID.randomUUID())
                        .contentType("application/json").content("{\"amount\": 2000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bidNotAboveTheHighestBidIsAConflict() throws Exception {
        UUID auctionId = runningAuction(seller());
        postBid(auctionId, buyer(), "1500").andExpect(status().isCreated());

        postBid(auctionId, buyer(), "1500")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:bid-too-low"))
                .andExpect(jsonPath("$.detail", containsString("1500.00")));
    }

    @Test
    void bidOnAnEndedAuctionIsAConflict() throws Exception {
        UUID auctionId = runningAuction(seller());
        closeAuction.handle(new CloseAuctionCommand(new AuctionId(auctionId)));

        postBid(auctionId, buyer(), "2000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:auction-not-open"));
    }
}
