package com.auction.infrastructure.events;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidPlaced;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire format of {@code auction.bids}; kept separate so the domain record can change without breaking consumers.
 */
record BidPlacedMessage(UUID auctionId, UUID bidId, UUID bidderId, BigDecimal amount, Instant occurredAt) {

    static BidPlacedMessage from(BidPlaced event) {
        return new BidPlacedMessage(event.auctionId().value(), event.bidId().value(), event.bidderId().value(),
                event.amount().amount(), event.occurredAt());
    }

    BidPlaced toEvent() {
        return new BidPlaced(new AuctionId(auctionId), new BidId(bidId), new UserId(bidderId), new Money(amount),
                occurredAt);
    }
}
