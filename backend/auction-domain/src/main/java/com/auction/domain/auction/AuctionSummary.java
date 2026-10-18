package com.auction.domain.auction;

import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.time.Instant;
import java.time.Year;

/**
 * Read model for auction listings. {@code currentHighestBid} is null when no bid was placed yet.
 */
public record AuctionSummary(
        AuctionId id,
        UserId sellerId,
        String carName,
        CarCondition condition,
        Year yearMade,
        Year yearRegistered,
        Money startPrice,
        Money currentHighestBid,
        Instant startDate,
        Instant endDate) {
}
