package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.CarDetails;
import com.auction.domain.shared.Guard;
import com.auction.domain.user.User;
import com.auction.domain.user.UserNotFoundException;
import com.auction.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Opens a new auction for a SELLER.
 */
@RequiredArgsConstructor
public class CreateAuctionUseCase {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public AuctionId handle(CreateAuctionCommand command) {
        Guard.notNull(command, "command");
        User seller = userRepository.findById(command.sellerId())
                .orElseThrow(() -> new UserNotFoundException(command.sellerId()));

        CarDetails car = new CarDetails(command.carName(), command.condition(), command.yearMade(),
                command.yearRegistered(), command.description());
        Auction auction = Auction.create(seller, car, command.startPrice(), command.startDate(), command.endDate());

        return auctionRepository.save(auction).getId();
    }
}
