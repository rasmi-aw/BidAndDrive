package com.auction.domain.auction;

import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.time.Instant;
import java.time.Year;

/**
 * Read model for auction listings. {@code description} is optional; {@code currentHighestBid} is null when no bid was placed yet.
 */
public record AuctionSummary(
        AuctionId id,
        UserId sellerId,
        String carName,
        CarCondition condition,
        Year yearMade,
        Year yearRegistered,
        String description,
        Money startPrice,
        Money currentHighestBid,
        Instant startDate,
        Instant endDate,
        AuctionStatus status) {

    /**
     * Same window rule as {@link Auction#placeBid}: RUNNING and {@code now} within [startDate, endDate).
     */
    public boolean isOpenForBidding(Instant now) {
        return status == AuctionStatus.RUNNING && !now.isBefore(startDate) && now.isBefore(endDate);
    }
}
