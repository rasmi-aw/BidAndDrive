package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;

public class AuctionNotFoundException extends DomainException {

    public AuctionNotFoundException(AuctionId auctionId) {
        super("auction " + auctionId.value() + " not found");
    }
}
