package com.auction.app.web.auction;

import com.auction.domain.auction.Bid;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code createdAt} is absent in the response to placing a bid: the command only returns the new bid's id.
 */
@Relation(itemRelation = "bid", collectionRelation = "bids")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BidResponse(UUID id, UUID auctionId, UUID bidderId, BigDecimal amount, Instant createdAt) {

    static BidResponse from(Bid bid) {
        return new BidResponse(bid.id().value(), bid.auctionId().value(), bid.userId().value(),
                bid.givenPrice().amount(), bid.createdAt());
    }
}
