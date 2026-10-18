package com.auction.domain.auction;

import com.auction.domain.shared.DomainEvent;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Guard;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    @Getter(AccessLevel.NONE)
    private UserId highestBidderId;
    private final Instant startDate;
    private final Instant endDate;
    private AuctionStatus status;
    @Getter(AccessLevel.NONE)
    private UserId wonByUserId;
    /** Optimistic-locking version, maintained by the persistence adapter. */
    private final long version;
    /** Raised by business methods, handed out once by {@link #pullEvents()}; never persisted. */
    @Getter(AccessLevel.NONE)
    private final List<DomainEvent> events = new ArrayList<>();

    private Auction(AuctionId id, UserId sellerId, CarDetails car, Money startPrice, Money currentHighestBid,
                    UserId highestBidderId, Instant startDate, Instant endDate, AuctionStatus status,
                    UserId wonByUserId, long version) {
        this.id = Guard.notNull(id, "id");
        this.sellerId = Guard.notNull(sellerId, "sellerId");
        this.car = Guard.notNull(car, "car");
        this.startPrice = Guard.notNull(startPrice, "startPrice");
        this.currentHighestBid = currentHighestBid;
        this.highestBidderId = highestBidderId;
        this.startDate = Guard.notNull(startDate, "startDate");
        this.endDate = Guard.notNull(endDate, "endDate");
        this.status = Guard.notNull(status, "status");
        this.wonByUserId = wonByUserId;
        this.version = version;
        if (startPrice.isZero()) {
            throw new DomainValidationException("startPrice must be greater than 0");
        }
        if (!endDate.isAfter(startDate)) {
            throw new DomainValidationException("endDate (" + endDate + ") must be after startDate (" + startDate + ")");
        }
        if ((currentHighestBid == null) != (highestBidderId == null)) {
            throw new DomainValidationException("currentHighestBid and highestBidderId must be both set or both empty");
        }
        if (wonByUserId != null && status != AuctionStatus.ENDED) {
            throw new DomainValidationException("wonByUserId can only be set on an ENDED auction");
        }
    }

    public static Auction create(User seller, CarDetails car, Money startPrice, Instant startDate, Instant endDate) {
        Guard.notNull(seller, "seller");
        if (!seller.isSeller()) {
            throw new OnlySellerCanCreateAuctionException(seller.getId());
        }
        return new Auction(AuctionId.newId(), seller.getId(), car, startPrice, null, null, startDate, endDate,
                AuctionStatus.RUNNING, null, 0);
    }

    /**
     * Rebuilds an auction from storage. Structural invariants are checked; creation rules (seller role) are not.
     */
    public static Auction restore(AuctionId id, UserId sellerId, CarDetails car, Money startPrice,
                                  Money currentHighestBid, UserId highestBidderId, Instant startDate, Instant endDate,
                                  AuctionStatus status, UserId wonByUserId, long version) {
        return new Auction(id, sellerId, car, startPrice, currentHighestBid, highestBidderId, startDate, endDate,
                status, wonByUserId, version);
    }

    /**
     * Accepts a bid if the bidder is a BUYER other than the seller, the auction is RUNNING within
     * [startDate, endDate), and the amount is greater than both the start price and the current highest bid.
     */
    public Bid placeBid(User bidder, Money amount, Clock clock) {
        Guard.notNull(bidder, "bidder");
        Guard.notNull(amount, "amount");
        Instant now = Guard.notNull(clock, "clock").instant();

        if (bidder.getId().equals(sellerId)) {
            throw new SellerCannotBidOnOwnAuctionException(id);
        }
        if (!bidder.isBuyer()) {
            throw new OnlyBuyerCanBidException(bidder.getId());
        }
        ensureOpenForBidding(now);
        Money minimumExclusive = getCurrentHighestBid().map(startPrice::max).orElse(startPrice);
        if (!amount.isGreaterThan(minimumExclusive)) {
            throw new BidTooLowException(amount, minimumExclusive);
        }

        currentHighestBid = amount;
        highestBidderId = bidder.getId();
        Bid bid = new Bid(BidId.newId(), id, bidder.getId(), amount, now);
        events.add(new BidPlaced(id, bid.id(), bidder.getId(), amount, now));
        return bid;
    }

    /**
     * Ends the auction and awards it to the highest bidder, if any.
     */
    public void close(Clock clock) {
        Instant now = Guard.notNull(clock, "clock").instant();
        ensureRunning();
        status = AuctionStatus.ENDED;
        wonByUserId = highestBidderId;
        events.add(new AuctionClosed(id, wonByUserId, currentHighestBid, now));
    }

    /**
     * Returns the events raised since the last call and forgets them, so each is published once.
     */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public void cancel() {
        ensureRunning();
        status = AuctionStatus.CANCELLED;
    }

    private void ensureRunning() {
        if (status != AuctionStatus.RUNNING) {
            throw new AuctionNotRunningException(id, status);
        }
    }

    private void ensureOpenForBidding(Instant now) {
        if (status != AuctionStatus.RUNNING) {
            throw new AuctionNotOpenForBiddingException(id, "status is " + status);
        }
        if (now.isBefore(startDate)) {
            throw new AuctionNotOpenForBiddingException(id, "starts at " + startDate);
        }
        if (!now.isBefore(endDate)) {
            throw new AuctionNotOpenForBiddingException(id, "ended at " + endDate);
        }
    }

    public Optional<Money> getCurrentHighestBid() {
        return Optional.ofNullable(currentHighestBid);
    }

    public Optional<UserId> getHighestBidderId() {
        return Optional.ofNullable(highestBidderId);
    }

    public Optional<UserId> getWonByUserId() {
        return Optional.ofNullable(wonByUserId);
    }
}
