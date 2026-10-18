package com.auction.domain.auction;

import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;

/**
 * Read-side port for the bids of an auction; implemented by a persistence adapter.
 */
public interface BidReadRepository {

    /**
     * Bids of the auction, newest first.
     */
    Page<Bid> findByAuctionId(AuctionId auctionId, PageQuery pageQuery);
}
