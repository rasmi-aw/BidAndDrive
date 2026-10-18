package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.auction.CarDetails;
import com.auction.domain.shared.Money;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final User seller = User.register("Sam Seller", new Email("seller@example.com"), new Phone("+33600000001"), Role.SELLER, CLOCK);
    private final User buyer = User.register("Bob Buyer", new Email("buyer@example.com"), new Phone("+33600000002"), Role.BUYER, CLOCK);
    private final Auction auction = Auction.create(seller,
            new CarDetails("Peugeot 308", CarCondition.GOOD, Year.of(2018), Year.of(2019), "One owner"),
            Money.of("1000"), NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(7)));

    @Test
    void copiesEveryDomainFieldIntoTheEntity() {
        auction.placeBid(buyer, Money.of("1500"), CLOCK);
        auction.close();
        AuctionEntity entity = new AuctionEntity(auction.getId().value());

        AuctionMapper.copyToEntity(auction, entity);

        assertThat(entity.getSellerId()).isEqualTo(seller.getId().value());
        assertThat(entity.getCarName()).isEqualTo("Peugeot 308");
        assertThat(entity.getCarCondition()).isEqualTo(CarCondition.GOOD);
        assertThat(entity.getYearMade()).isEqualTo(2018);
        assertThat(entity.getYearRegistered()).isEqualTo(2019);
        assertThat(entity.getDescription()).isEqualTo("One owner");
        assertThat(entity.getStartPrice()).isEqualByComparingTo("1000");
        assertThat(entity.getCurrentHighestBid()).isEqualByComparingTo("1500");
        assertThat(entity.getHighestBidderId()).isEqualTo(buyer.getId().value());
        assertThat(entity.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(entity.getWonByUserId()).isEqualTo(buyer.getId().value());
        assertThat(entity.getVersion()).as("version is owned by Hibernate").isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void roundTripsThroughTheEntityIncludingVersion() {
        auction.placeBid(buyer, Money.of("1500"), CLOCK);
        AuctionEntity entity = new AuctionEntity(auction.getId().value());
        AuctionMapper.copyToEntity(auction, entity);
        entity.setVersion(4L);

        Auction restored = AuctionMapper.toDomain(entity);

        assertThat(restored).usingRecursiveComparison().ignoringFields("version").isEqualTo(auction);
        assertThat(restored.getVersion()).isEqualTo(4);
    }

    @Test
    void mapsAnAuctionWithoutBids() {
        AuctionEntity entity = new AuctionEntity(auction.getId().value());
        AuctionMapper.copyToEntity(auction, entity);
        entity.setVersion(0L);

        Auction restored = AuctionMapper.toDomain(entity);

        assertThat(restored.getCurrentHighestBid()).isEmpty();
        assertThat(restored.getHighestBidderId()).isEmpty();
        assertThat(restored.getWonByUserId()).isEmpty();
    }

    @Test
    void mapsToSummary() {
        AuctionEntity entity = new AuctionEntity(auction.getId().value());
        AuctionMapper.copyToEntity(auction, entity);
        entity.setCurrentHighestBid(new BigDecimal("1200.00"));

        AuctionSummary summary = AuctionMapper.toSummary(entity);

        assertThat(summary.id()).isEqualTo(auction.getId());
        assertThat(summary.sellerId()).isEqualTo(seller.getId());
        assertThat(summary.carName()).isEqualTo("Peugeot 308");
        assertThat(summary.condition()).isEqualTo(CarCondition.GOOD);
        assertThat(summary.yearMade()).isEqualTo(Year.of(2018));
        assertThat(summary.yearRegistered()).isEqualTo(Year.of(2019));
        assertThat(summary.description()).isEqualTo("One owner");
        assertThat(summary.startPrice()).isEqualTo(Money.of("1000"));
        assertThat(summary.currentHighestBid()).isEqualTo(Money.of("1200"));
        assertThat(summary.startDate()).isEqualTo(auction.getStartDate());
        assertThat(summary.endDate()).isEqualTo(auction.getEndDate());
    }

    @Test
    void mapsBidToEntityAndBack() {
        Bid bid = auction.placeBid(buyer, Money.of("1500"), CLOCK);

        BidEntity entity = BidMapper.toEntity(bid);

        assertThat(entity.getAuctionId()).isEqualTo(auction.getId().value());
        assertThat(entity.getUserId()).isEqualTo(buyer.getId().value());
        assertThat(entity.isDeleted()).isFalse();
        assertThat(BidMapper.toDomain(entity)).isEqualTo(bid);
    }
}
