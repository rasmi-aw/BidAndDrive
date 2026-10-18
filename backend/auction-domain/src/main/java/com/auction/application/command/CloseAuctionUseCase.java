package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.shared.Guard;
import lombok.RequiredArgsConstructor;

/**
 * Ends a RUNNING auction and awards it to the highest bidder, if any.
 */
@RequiredArgsConstructor
public class CloseAuctionUseCase {

    private final AuctionRepository auctionRepository;

    public void handle(CloseAuctionCommand command) {
        Guard.notNull(command, "command");
        Auction auction = auctionRepository.findById(command.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException(command.auctionId()));

        auction.close();

        auctionRepository.save(auction);
    }
}
