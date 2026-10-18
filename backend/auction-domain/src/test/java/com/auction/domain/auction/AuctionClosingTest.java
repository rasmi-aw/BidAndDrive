package com.auction.domain.auction;

import com.auction.domain.shared.Money;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.clockAt;
import static com.auction.domain.DomainFixtures.runningAuction;
import static com.auction.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionClosingTest {

    private static final Clock CLOCK = clockAt(NOW);

    private final User buyer = buyer();
    private final Auction auction = runningAuction(seller());

    @Test
    void closingEndsTheAuctionAndAwardsItToTheHighestBidder() {
        User topBidder = User.register("Alice Buyer", new Email("alice@example.com"), new Phone("+33600000004"), Role.BUYER, CLOCK);
        auction.placeBid(buyer, Money.of("1500"), CLOCK);
        auction.placeBid(topBidder, Money.of("1800"), CLOCK);

        auction.close(CLOCK);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(auction.getWonByUserId()).contains(topBidder.getId());
    }

    @Test
    void closingWithoutBidsEndsTheAuctionWithNoWinner() {
        auction.close(CLOCK);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(auction.getWonByUserId()).isEmpty();
    }

    @Test
    void cannotCloseAnAuctionTwice() {
        auction.close(CLOCK);

        assertThatThrownBy(() -> auction.close(CLOCK)).isInstanceOf(AuctionNotRunningException.class);
    }

    @Test
    void cancellingARunningAuctionMarksItCancelledWithNoWinner() {
        auction.placeBid(buyer, Money.of("1500"), CLOCK);

        auction.cancel();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
        assertThat(auction.getWonByUserId()).isEmpty();
    }

    @Test
    void cannotCloseACancelledAuctionOrCancelAnEndedOne() {
        Auction cancelled = runningAuction(seller());
        cancelled.cancel();

        assertThatThrownBy(() -> cancelled.close(CLOCK)).isInstanceOf(AuctionNotRunningException.class);
        assertThatThrownBy(cancelled::cancel).isInstanceOf(AuctionNotRunningException.class);

        auction.close(CLOCK);
        assertThatThrownBy(auction::cancel).isInstanceOf(AuctionNotRunningException.class);
    }

    @Test
    void cannotBidOnAnEndedAuction() {
        auction.close(CLOCK);

        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("2000"), CLOCK))
                .isInstanceOf(AuctionNotOpenForBiddingException.class)
                .hasMessageContaining("ENDED");
    }

    @Test
    void cannotBidOnACancelledAuction() {
        auction.cancel();

        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("2000"), CLOCK))
                .isInstanceOf(AuctionNotOpenForBiddingException.class)
                .hasMessageContaining("CANCELLED");
    }
}
