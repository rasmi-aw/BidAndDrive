package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;

/**
 * Raised by the persistence adapter when an auction was changed by someone else since it was loaded
 * (optimistic locking). The caller may reload and retry.
 */
public class AuctionConcurrentlyModifiedException extends DomainException {

    public AuctionConcurrentlyModifiedException(AuctionId auctionId) {
        super("auction " + auctionId.value() + " was modified concurrently, reload and retry");
    }
}
