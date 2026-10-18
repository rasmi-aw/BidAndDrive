package com.auction.domain.auction;

import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.time.Instant;

/**
 * An accepted bid. Created by {@link Auction#placeBid}; immutable.
 */
public record Bid(BidId id, AuctionId auctionId, UserId userId, Money givenPrice, Instant createdAt) {

    public Bid {
        Guard.notNull(id, "id");
        Guard.notNull(auctionId, "auctionId");
        Guard.notNull(userId, "userId");
        Guard.notNull(givenPrice, "givenPrice");
        Guard.notNull(createdAt, "createdAt");
    }
}
