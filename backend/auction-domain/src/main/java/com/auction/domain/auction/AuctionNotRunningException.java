package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;

public class AuctionNotRunningException extends DomainException {

    public AuctionNotRunningException(AuctionId auctionId, AuctionStatus status) {
        super("auction " + auctionId.value() + " is " + status + ", expected RUNNING");
    }
}
