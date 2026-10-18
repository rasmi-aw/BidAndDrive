package com.auction.domain.auction;

import com.auction.domain.shared.DomainEvent;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.time.Instant;
import java.util.Optional;

/**
 * The auction ended. {@code winnerId} and {@code winningAmount} are null when nobody bid.
 */
public record AuctionClosed(AuctionId auctionId, UserId winnerId, Money winningAmount, Instant occurredAt)
        implements DomainEvent {

    public AuctionClosed {
        Guard.notNull(auctionId, "auctionId");
        Guard.notNull(occurredAt, "occurredAt");
    }

    public Optional<UserId> winner() {
        return Optional.ofNullable(winnerId);
    }

    public Optional<Money> winningBid() {
        return Optional.ofNullable(winningAmount);
    }
}
