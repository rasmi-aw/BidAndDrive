package com.auction.application.command;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.shared.Guard;

public record CloseAuctionCommand(AuctionId auctionId) {

    public CloseAuctionCommand {
        Guard.notNull(auctionId, "auctionId");
    }
}
