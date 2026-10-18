package com.auction.infrastructure.events;

/**
 * Kafka topics for auction events. Records are keyed by auction id, so the events of one auction stay in order.
 */
public final class AuctionTopics {

    public static final String BIDS = "auction.bids";
    public static final String CLOSED = "auction.closed";

    private AuctionTopics() {
    }
}
