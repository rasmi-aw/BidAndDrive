package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;

public class SellerCannotBidOnOwnAuctionException extends DomainException {

    public SellerCannotBidOnOwnAuctionException(AuctionId auctionId) {
        super("the seller cannot bid on their own auction " + auctionId.value());
    }
}
