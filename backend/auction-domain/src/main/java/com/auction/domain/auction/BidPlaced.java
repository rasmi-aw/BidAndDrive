package com.auction.domain.auction;

import com.auction.domain.shared.DomainEvent;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.time.Instant;

/**
 * A bid was accepted and is now the highest bid of the auction.
 */
public record BidPlaced(AuctionId auctionId, BidId bidId, UserId bidderId, Money amount, Instant occurredAt)
        implements DomainEvent {

    public BidPlaced {
        Guard.notNull(auctionId, "auctionId");
        Guard.notNull(bidId, "bidId");
        Guard.notNull(bidderId, "bidderId");
        Guard.notNull(amount, "amount");
        Guard.notNull(occurredAt, "occurredAt");
    }
}
