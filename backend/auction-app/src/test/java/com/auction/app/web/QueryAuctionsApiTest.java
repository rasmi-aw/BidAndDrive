package com.auction.app.web;

import com.auction.application.command.CloseAuctionCommand;
import com.auction.application.command.CloseAuctionUseCase;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueryAuctionsApiTest extends ApiIntegrationTest {

    @Autowired
    private CloseAuctionUseCase closeAuction;

    @Test
    void runningAuctionsArePublicPagedAndEndingSoonestFirst() throws Exception {
        User seller = seller();
        UUID third = runningAuction(seller, Duration.ofDays(3));
        UUID first = runningAuction(seller, Duration.ofDays(1));
        UUID second = runningAuction(seller, Duration.ofDays(2));
        UUID closed = runningAuction(seller, Duration.ofDays(1));
        closeAuction.handle(new CloseAuctionCommand(new AuctionId(closed)));

        mvc.perform(get("/api/auctions/running").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
                .andExpect(jsonPath("$._embedded.auctions[*].id", contains(first.toString(), second.toString())))
                .andExpect(jsonPath("$._embedded.auctions[*]._links.place-bid.href", everyItem(endsWith("/bids"))))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$._links.self.href", containsString("page=0")))
                .andExpect(jsonPath("$._links.next.href", containsString("page=1")))
                .andExpect(jsonPath("$._links.prev").doesNotExist());

        mvc.perform(get("/api/auctions/running").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$._embedded.auctions[*].id", contains(third.toString())))
                .andExpect(jsonPath("$._links.prev.href", containsString("page=0")))
                .andExpect(jsonPath("$._links.next").doesNotExist());
    }

    @Test
    void runningAuctionsUseDefaultPaging() throws Exception {
        runningAuction(seller());

        mvc.perform(get("/api/auctions/running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void invalidPagingIsABadRequest() throws Exception {
        mvc.perform(get("/api/auctions/running").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
        mvc.perform(get("/api/auctions/running").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsOneAuctionWithLinks() throws Exception {
        UUID id = runningAuction(seller());

        mvc.perform(get("/api/auctions/{id}", id).with(anyToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/auctions/" + id)))
                .andExpect(jsonPath("$._links.bids.href", endsWith("/api/auctions/" + id + "/bids")))
                .andExpect(jsonPath("$._links.place-bid.href", endsWith("/api/auctions/" + id + "/bids")));
    }

    @Test
    void endedAuctionHasNoPlaceBidLink() throws Exception {
        UUID id = runningAuction(seller());
        closeAuction.handle(new CloseAuctionCommand(new AuctionId(id)));

        mvc.perform(get("/api/auctions/{id}", id).with(anyToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.bids").exists())
                .andExpect(jsonPath("$._links.place-bid").doesNotExist());
    }

    @Test
    void unknownAuctionIsNotFound() throws Exception {
        mvc.perform(get("/api/auctions/{id}", UUID.randomUUID()).with(anyToken()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:not-found"));
    }

    @Test
    void malformedAuctionIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/auctions/{id}", "not-a-uuid").with(anyToken()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }
}
