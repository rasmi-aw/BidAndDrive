package com.auction.application.query;

import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Page;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

/**
 * Lists auctions currently open for bidding, as of the injected Clock. Read-only.
 */
@RequiredArgsConstructor
public class ListRunningAuctionsUseCase {

    private final AuctionReadRepository auctionReadRepository;
    private final Clock clock;

    public Page<AuctionSummary> handle(ListRunningAuctionsQuery query) {
        Guard.notNull(query, "query");
        return auctionReadRepository.findRunning(clock.instant(), query.pageQuery());
    }
}
