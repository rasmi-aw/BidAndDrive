package com.auction.domain.auction;

import com.auction.domain.shared.Guard;

import java.util.UUID;

public record BidId(UUID value) {

    public BidId {
        Guard.notNull(value, "bidId");
    }

    public static BidId newId() {
        return new BidId(UUID.randomUUID());
    }
}
