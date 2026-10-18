package com.auction.infrastructure.events;

import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire format of {@code auction.closed}; {@code winnerId} and {@code winningBid} are null when nobody bid.
 */
record AuctionClosedMessage(UUID auctionId, UUID winnerId, BigDecimal winningBid, Instant occurredAt) {

    static AuctionClosedMessage from(AuctionClosed event) {
        return new AuctionClosedMessage(event.auctionId().value(),
                event.winner().map(UserId::value).orElse(null),
                event.winningBid().map(Money::amount).orElse(null),
                event.occurredAt());
    }

    AuctionClosed toEvent() {
        return new AuctionClosed(new AuctionId(auctionId), winnerId == null ? null : new UserId(winnerId),
                winningBid == null ? null : new Money(winningBid), occurredAt);
    }
}
