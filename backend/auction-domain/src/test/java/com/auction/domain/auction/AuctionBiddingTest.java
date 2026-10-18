package com.auction.domain.auction;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.START;
import static com.auction.domain.DomainFixtures.START_PRICE;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.clockAt;
import static com.auction.domain.DomainFixtures.runningAuction;
import static com.auction.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionBiddingTest {

    private static final Clock CLOCK = clockAt(NOW);

    private final User seller = seller();
    private final User buyer = buyer();
    private final Auction auction = runningAuction(seller);

    @Test
    void buyerBidAboveStartPriceIsAcceptedAndBecomesTheHighestBid() {
        Bid bid = auction.placeBid(buyer, Money.of("1000.01"), CLOCK);

        assertThat(bid.id()).isNotNull();
        assertThat(bid.auctionId()).isEqualTo(auction.getId());
        assertThat(bid.userId()).isEqualTo(buyer.getId());
        assertThat(bid.givenPrice()).isEqualTo(Money.of("1000.01"));
        assertThat(bid.createdAt()).isEqualTo(NOW);
        assertThat(auction.getCurrentHighestBid()).contains(Money.of("1000.01"));
        assertThat(auction.getHighestBidderId()).contains(buyer.getId());
    }

    @Test
    void bidEqualToStartPriceIsRejected() {
        assertThatThrownBy(() -> auction.placeBid(buyer, START_PRICE, CLOCK))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    void bidBelowStartPriceIsRejected() {
        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("999.99"), CLOCK))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    void nextBidMustBeatTheCurrentHighestBid() {
        User otherBuyer = otherBuyer();
        auction.placeBid(buyer, Money.of("1500"), CLOCK);

        assertThatThrownBy(() -> auction.placeBid(otherBuyer, Money.of("1500"), CLOCK))
                .isInstanceOf(BidTooLowException.class);
        assertThatThrownBy(() -> auction.placeBid(otherBuyer, Money.of("1200"), CLOCK))
                .isInstanceOf(BidTooLowException.class);

        auction.placeBid(otherBuyer, Money.of("1500.50"), CLOCK);

        assertThat(auction.getCurrentHighestBid()).contains(Money.of("1500.50"));
        assertThat(auction.getHighestBidderId()).contains(otherBuyer.getId());
    }

    @Test
    void sellerCannotBidOnTheirOwnAuction() {
        assertThatThrownBy(() -> auction.placeBid(seller, Money.of("2000"), CLOCK))
                .isInstanceOf(SellerCannotBidOnOwnAuctionException.class);
    }

    @Test
    void anotherSellerCannotBid() {
        User otherSeller = User.register("Other Seller", new Email("other.seller@example.com"),
                new Phone("+33600000003"), Role.SELLER, CLOCK);

        assertThatThrownBy(() -> auction.placeBid(otherSeller, Money.of("2000"), CLOCK))
                .isInstanceOf(OnlyBuyerCanBidException.class);
    }

    @Test
    void cannotBidOnceEndDateHasPassedEvenIfStillRunning() {
        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("2000"), clockAt(END.plus(Duration.ofSeconds(1)))))
                .isInstanceOf(AuctionNotOpenForBiddingException.class);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    void cannotBidExactlyAtEndDate() {
        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("2000"), clockAt(END)))
                .isInstanceOf(AuctionNotOpenForBiddingException.class);
    }

    @Test
    void cannotBidBeforeStartDate() {
        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("2000"), clockAt(START.minus(Duration.ofSeconds(1)))))
                .isInstanceOf(AuctionNotOpenForBiddingException.class);
    }

    @Test
    void canBidExactlyAtStartDate() {
        assertThat(auction.placeBid(buyer, Money.of("2000"), clockAt(START)).createdAt()).isEqualTo(START);
    }

    @Test
    void rejectedBidLeavesTheAuctionUnchanged() {
        auction.placeBid(buyer, Money.of("1500"), CLOCK);

        assertThatThrownBy(() -> auction.placeBid(otherBuyer(), Money.of("1400"), CLOCK))
                .isInstanceOf(BidTooLowException.class);

        assertThat(auction.getCurrentHighestBid()).contains(Money.of("1500"));
        assertThat(auction.getHighestBidderId()).contains(buyer.getId());
    }

    @Test
    void requiresBidderAmountAndClock() {
        assertThatThrownBy(() -> auction.placeBid(null, Money.of("2000"), CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("bidder");
        assertThatThrownBy(() -> auction.placeBid(buyer, null, CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("amount");
        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("2000"), null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("clock");
    }

    private static User otherBuyer() {
        return User.register("Alice Buyer", new Email("alice@example.com"), new Phone("+33600000004"), Role.BUYER, CLOCK);
    }
}
