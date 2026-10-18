package com.auction.domain.auction;

import java.util.Optional;

/**
 * Write-side port for the {@link Auction} aggregate; implemented by a persistence adapter.
 */
public interface AuctionRepository {

    Auction save(Auction auction);

    Optional<Auction> findById(AuctionId id);
}
