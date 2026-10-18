package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;

public class AuctionNotOpenForBiddingException extends DomainException {

    public AuctionNotOpenForBiddingException(AuctionId auctionId, String reason) {
        super("auction " + auctionId.value() + " is not open for bidding: " + reason);
    }
}
