package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.auction.CarDetails;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

import java.math.BigDecimal;
import java.time.Year;
import java.util.UUID;

final class AuctionMapper {

    private AuctionMapper() {
    }

    /**
     * Copies domain state onto a new or managed entity. {@code version} and {@code deleted} are left untouched.
     */
    static void copyToEntity(Auction auction, AuctionEntity entity) {
        CarDetails car = auction.getCar();
        entity.setSellerId(auction.getSellerId().value());
        entity.setCarName(car.carName());
        entity.setCarCondition(car.condition());
        entity.setYearMade(car.yearMade().getValue());
        entity.setYearRegistered(car.yearRegistered().getValue());
        entity.setDescription(car.description());
        entity.setStartPrice(auction.getStartPrice().amount());
        entity.setCurrentHighestBid(auction.getCurrentHighestBid().map(Money::amount).orElse(null));
        entity.setHighestBidderId(auction.getHighestBidderId().map(UserId::value).orElse(null));
        entity.setStartDate(auction.getStartDate());
        entity.setEndDate(auction.getEndDate());
        entity.setStatus(auction.getStatus());
        entity.setWonByUserId(auction.getWonByUserId().map(UserId::value).orElse(null));
    }

    static Auction toDomain(AuctionEntity entity) {
        return Auction.restore(
                new AuctionId(entity.getId()),
                new UserId(entity.getSellerId()),
                carDetails(entity),
                new Money(entity.getStartPrice()),
                money(entity.getCurrentHighestBid()),
                userId(entity.getHighestBidderId()),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                userId(entity.getWonByUserId()),
                entity.getVersion());
    }

    static AuctionSummary toSummary(AuctionEntity entity) {
        return new AuctionSummary(
                new AuctionId(entity.getId()),
                new UserId(entity.getSellerId()),
                entity.getCarName(),
                entity.getCarCondition(),
                Year.of(entity.getYearMade()),
                Year.of(entity.getYearRegistered()),
                entity.getDescription(),
                new Money(entity.getStartPrice()),
                money(entity.getCurrentHighestBid()),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus());
    }

    private static CarDetails carDetails(AuctionEntity entity) {
        return new CarDetails(entity.getCarName(), entity.getCarCondition(), Year.of(entity.getYearMade()),
                Year.of(entity.getYearRegistered()), entity.getDescription());
    }

    private static Money money(BigDecimal amount) {
        return amount == null ? null : new Money(amount);
    }

    private static UserId userId(UUID id) {
        return id == null ? null : new UserId(id);
    }
}
