package com.auction.app.web;

import com.auction.app.web.stream.AuctionStreamBroadcaster;
import com.auction.application.command.CloseAuctionCommand;
import com.auction.application.command.CloseAuctionUseCase;
import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidPlaced;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Kafka consumer is off in these H2 tests: events are handed to the broadcaster directly, like the consumer does.
 */
class AuctionStreamApiTest extends ApiIntegrationTest {

    private static final Instant AT = Instant.parse("2026-10-18T10:15:30Z");

    @Autowired
    private AuctionStreamBroadcaster broadcaster;
    @Autowired
    private CloseAuctionUseCase closeAuction;

    @Test
    void streamsNewBidsOfTheAuctionAsServerSentEvents() throws Exception {
        UUID id = runningAuction(seller());
        MvcResult stream = openStream(id);

        BidPlaced bid = new BidPlaced(new AuctionId(id), BidId.newId(), UserId.newId(), Money.of("1500"), AT);
        broadcaster.onBidPlaced(bid);

        String body = stream.getResponse().getContentAsString();
        assertThat(stream.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(stream.getResponse().getHeader("X-Accel-Buffering")).isEqualTo("no");
        assertThat(body).contains("event:connected");
        assertThat(body).contains("event:bid", "id:" + bid.bidId().value());
        assertThat(body).contains("\"auctionId\":\"" + id + "\"", "\"amount\":1500.00", "\"occurredAt\":\"2026-10-18T10:15:30Z\"");
    }

    @Test
    void doesNotLeakBidsOfOtherAuctions() throws Exception {
        UUID id = runningAuction(seller());
        MvcResult stream = openStream(id);

        broadcaster.onBidPlaced(new BidPlaced(AuctionId.newId(), BidId.newId(), UserId.newId(), Money.of("9999"), AT));

        assertThat(stream.getResponse().getContentAsString()).doesNotContain("event:bid");
    }

    @Test
    void sendsTheClosingAndEndsTheStream() throws Exception {
        UUID id = runningAuction(seller());
        MvcResult stream = openStream(id);
        UserId winner = UserId.newId();

        broadcaster.onAuctionClosed(new AuctionClosed(new AuctionId(id), winner, Money.of("2000"), AT));

        assertThat(stream.getResponse().getContentAsString())
                .contains("event:closed", "\"winnerId\":\"" + winner.value() + "\"", "\"winningBid\":2000.00");
        assertThat(broadcaster.subscriberCount(new AuctionId(id))).isZero();
    }

    @Test
    void heartbeatsKeepIdleStreamsOpen() throws Exception {
        UUID id = runningAuction(seller());
        MvcResult stream = openStream(id);

        broadcaster.heartbeat();

        assertThat(stream.getResponse().getContentAsString()).contains(":heartbeat");
        assertThat(broadcaster.subscriberCount(new AuctionId(id))).isEqualTo(1);
    }

    @Test
    void unknownAuctionIsNotFound() throws Exception {
        mvc.perform(get("/api/auctions/{id}/stream", UUID.randomUUID()).with(anyToken())
                        .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void needsAToken() throws Exception {
        mvc.perform(get("/api/auctions/{id}/stream", UUID.randomUUID()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runningAuctionsLinkToTheirStreamEndedOnesDoNot() throws Exception {
        UUID id = runningAuction(seller());

        mvc.perform(get("/api/auctions/{id}", id).with(anyToken()))
                .andExpect(jsonPath("$._links.stream.href", endsWith("/api/auctions/" + id + "/stream")));

        closeAuction.handle(new CloseAuctionCommand(new AuctionId(id)));
        mvc.perform(get("/api/auctions/{id}", id).with(anyToken()))
                .andExpect(jsonPath("$._links.stream").doesNotExist());
    }

    private MvcResult openStream(UUID auctionId) throws Exception {
        return mvc.perform(get("/api/auctions/{id}/stream", auctionId).with(anyToken())
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();
    }
}
