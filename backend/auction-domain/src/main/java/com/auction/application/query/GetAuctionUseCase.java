package com.auction.application.query;

import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.shared.Guard;
import lombok.RequiredArgsConstructor;

/**
 * Loads one auction by id. Read-only.
 */
@RequiredArgsConstructor
public class GetAuctionUseCase {

    private final AuctionReadRepository auctionReadRepository;

    public AuctionSummary handle(GetAuctionQuery query) {
        Guard.notNull(query, "query");
        return auctionReadRepository.findById(query.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException(query.auctionId()));
    }
}
