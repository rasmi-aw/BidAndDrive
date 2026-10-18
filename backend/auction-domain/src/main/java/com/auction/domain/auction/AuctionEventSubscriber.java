package com.auction.domain.auction;

/**
 * Inbound port: notified of auction events coming back from the event bus (e.g. to push them to browsers).
 * Implementations must be fast and must not throw; they run on the consumer thread.
 */
public interface AuctionEventSubscriber {

    void onBidPlaced(BidPlaced event);

    void onAuctionClosed(AuctionClosed event);
}
