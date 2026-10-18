package com.auction.domain.auction;

import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;

import java.time.Instant;
import java.util.Optional;

/**
 * Read-side port returning {@link AuctionSummary} projections; implemented by a persistence adapter.
 */
public interface AuctionReadRepository {

    Optional<AuctionSummary> findById(AuctionId id);

    /**
     * Auctions with status RUNNING whose bidding window contains {@code now}, ordered by endDate ascending.
     */
    Page<AuctionSummary> findRunning(Instant now, PageQuery pageQuery);
}
