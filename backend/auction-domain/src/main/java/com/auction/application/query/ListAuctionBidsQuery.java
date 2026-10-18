package com.auction.application.query;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.PageQuery;

public record ListAuctionBidsQuery(AuctionId auctionId, PageQuery pageQuery) {

    public ListAuctionBidsQuery {
        Guard.notNull(auctionId, "auctionId");
        Guard.notNull(pageQuery, "pageQuery");
    }

    public static ListAuctionBidsQuery of(AuctionId auctionId, int page, int size) {
        return new ListAuctionBidsQuery(auctionId, new PageQuery(page, size));
    }
}
