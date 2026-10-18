package com.auction.domain.auction;

/**
 * Write-side port for accepted {@link Bid}s; implemented by a persistence adapter.
 */
public interface BidRepository {

    Bid save(Bid bid);
}
