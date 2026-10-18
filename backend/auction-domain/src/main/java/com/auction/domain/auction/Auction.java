package com.auction.domain.auction;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

/**
 * Auction aggregate root. State only changes through business methods; there are no setters.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Auction {

    @EqualsAndHashCode.Include
    private final AuctionId id;
    private final UserId sellerId;
    private final CarDetails car;
    private final Money startPrice;
    @Getter(AccessLevel.NONE)
    private Money currentHighestBid;
    private final Instant startDate;
    private final Instant endDate;
    private AuctionStatus status;
    @Getter(AccessLevel.NONE)
    private UserId wonByUserId;
    /** Optimistic-locking version, maintained by the persistence adapter. */
    private final long version;

    private Auction(AuctionId id, UserId sellerId, CarDetails car, Money startPrice, Instant startDate, Instant endDate,
                    AuctionStatus status, long version) {
        this.id = Guard.notNull(id, "id");
        this.sellerId = Guard.notNull(sellerId, "sellerId");
        this.car = Guard.notNull(car, "car");
        this.startPrice = Guard.notNull(startPrice, "startPrice");
        this.startDate = Guard.notNull(startDate, "startDate");
        this.endDate = Guard.notNull(endDate, "endDate");
        this.status = Guard.notNull(status, "status");
        this.version = version;
        if (startPrice.isZero()) {
            throw new DomainValidationException("startPrice must be greater than 0");
        }
        if (!endDate.isAfter(startDate)) {
            throw new DomainValidationException("endDate (" + endDate + ") must be after startDate (" + startDate + ")");
        }
    }

    public static Auction create(User seller, CarDetails car, Money startPrice, Instant startDate, Instant endDate) {
        Guard.notNull(seller, "seller");
        if (!seller.isSeller()) {
            throw new OnlySellerCanCreateAuctionException(seller.getId());
        }
        return new Auction(AuctionId.newId(), seller.getId(), car, startPrice, startDate, endDate, AuctionStatus.RUNNING, 0);
    }

    public Optional<Money> getCurrentHighestBid() {
        return Optional.ofNullable(currentHighestBid);
    }

    public Optional<UserId> getWonByUserId() {
        return Optional.ofNullable(wonByUserId);
    }
}
