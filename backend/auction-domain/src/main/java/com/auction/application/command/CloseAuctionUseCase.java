package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.shared.DomainEventPublisher;
import com.auction.domain.shared.Guard;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

/**
 * Ends a RUNNING auction, awards it to the highest bidder (if any) and publishes {@code AuctionClosed}.
 */
@RequiredArgsConstructor
public class CloseAuctionUseCase {

    private final AuctionRepository auctionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public void handle(CloseAuctionCommand command) {
        Guard.notNull(command, "command");
        Auction auction = auctionRepository.findById(command.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException(command.auctionId()));

        auction.close(clock);

        auctionRepository.save(auction);
        auction.pullEvents().forEach(eventPublisher::publish);
    }
}
