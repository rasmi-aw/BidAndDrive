package com.auction.application.command;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

public record PlaceBidCommand(AuctionId auctionId, UserId bidderId, Money amount) {

    public PlaceBidCommand {
        Guard.notNull(auctionId, "auctionId");
        Guard.notNull(bidderId, "bidderId");
    }
}
