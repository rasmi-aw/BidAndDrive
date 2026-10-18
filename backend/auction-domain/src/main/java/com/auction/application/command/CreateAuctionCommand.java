package com.auction.application.command;

import com.auction.domain.auction.CarCondition;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.time.Instant;
import java.time.Year;

public record CreateAuctionCommand(
        UserId sellerId,
        String carName,
        CarCondition condition,
        Year yearMade,
        Year yearRegistered,
        String description,
        Money startPrice,
        Instant startDate,
        Instant endDate) {

    public CreateAuctionCommand {
        Guard.notNull(sellerId, "sellerId");
    }
}
