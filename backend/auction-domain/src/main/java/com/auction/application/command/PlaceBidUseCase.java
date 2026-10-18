package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidRepository;
import com.auction.domain.shared.Guard;
import com.auction.domain.user.User;
import com.auction.domain.user.UserNotFoundException;
import com.auction.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

/**
 * Places a bid on an auction. The auction is saved before the bid so a concurrent update fails on its version
 * before anything else is written.
 */
@RequiredArgsConstructor
public class PlaceBidUseCase {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final Clock clock;

    public BidId handle(PlaceBidCommand command) {
        Guard.notNull(command, "command");
        Auction auction = auctionRepository.findById(command.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException(command.auctionId()));
        User bidder = userRepository.findById(command.bidderId())
                .orElseThrow(() -> new UserNotFoundException(command.bidderId()));

        Bid bid = auction.placeBid(bidder, command.amount(), clock);

        auctionRepository.save(auction);
        return bidRepository.save(bid).id();
    }
}
