package com.auction.domain.auction;

import com.auction.domain.shared.Guard;

import java.util.UUID;

public record AuctionId(UUID value) {

    public AuctionId {
        Guard.notNull(value, "auctionId");
    }

    public static AuctionId newId() {
        return new AuctionId(UUID.randomUUID());
    }
}
