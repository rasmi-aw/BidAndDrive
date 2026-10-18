package com.auction.application.query;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.shared.Guard;

public record GetAuctionQuery(AuctionId auctionId) {

    public GetAuctionQuery {
        Guard.notNull(auctionId, "auctionId");
    }
}
