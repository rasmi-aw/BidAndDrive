package com.auction.application.query;

import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidReadRepository;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Page;
import lombok.RequiredArgsConstructor;

/**
 * Lists the bids of an auction, newest first. Read-only.
 */
@RequiredArgsConstructor
public class ListAuctionBidsUseCase {

    private final AuctionReadRepository auctionReadRepository;
    private final BidReadRepository bidReadRepository;

    public Page<Bid> handle(ListAuctionBidsQuery query) {
        Guard.notNull(query, "query");
        if (auctionReadRepository.findById(query.auctionId()).isEmpty()) {
            throw new AuctionNotFoundException(query.auctionId());
        }
        return bidReadRepository.findByAuctionId(query.auctionId(), query.pageQuery());
    }
}
