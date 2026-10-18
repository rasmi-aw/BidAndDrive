package com.auction.app.web.stream;

import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.BidPlaced;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JSON payloads of the auction stream (SSE event names: connected, bid, closed).
 */
final class StreamMessages {

    private StreamMessages() {
    }

    record Connected(UUID auctionId) {
    }

    record BidMessage(UUID auctionId, UUID bidId, UUID bidderId, BigDecimal amount, Instant occurredAt) {

        static BidMessage from(BidPlaced event) {
            return new BidMessage(event.auctionId().value(), event.bidId().value(), event.bidderId().value(),
                    event.amount().amount(), event.occurredAt());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ClosedMessage(UUID auctionId, UUID winnerId, BigDecimal winningBid, Instant occurredAt) {

        static ClosedMessage from(AuctionClosed event) {
            return new ClosedMessage(event.auctionId().value(), event.winner().map(UserId::value).orElse(null),
                    event.winningBid().map(Money::amount).orElse(null), event.occurredAt());
        }
    }
}
