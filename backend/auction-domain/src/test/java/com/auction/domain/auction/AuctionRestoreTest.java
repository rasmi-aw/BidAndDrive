package com.auction.domain.auction;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.Test;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.START;
import static com.auction.domain.DomainFixtures.START_PRICE;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.car;
import static com.auction.domain.DomainFixtures.clockAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionRestoreTest {

    private final AuctionId id = AuctionId.newId();
    private final UserId sellerId = UserId.newId();
    private final UserId bidderId = UserId.newId();

    @Test
    void restoresEveryFieldAsStored() {
        Auction auction = Auction.restore(id, sellerId, car(), START_PRICE, Money.of("1500"), bidderId,
                START, END, AuctionStatus.ENDED, bidderId, 7);

        assertThat(auction.getId()).isEqualTo(id);
        assertThat(auction.getSellerId()).isEqualTo(sellerId);
        assertThat(auction.getCar()).isEqualTo(car());
        assertThat(auction.getStartPrice()).isEqualTo(START_PRICE);
        assertThat(auction.getCurrentHighestBid()).contains(Money.of("1500"));
        assertThat(auction.getHighestBidderId()).contains(bidderId);
        assertThat(auction.getStartDate()).isEqualTo(START);
        assertThat(auction.getEndDate()).isEqualTo(END);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(auction.getWonByUserId()).contains(bidderId);
        assertThat(auction.getVersion()).isEqualTo(7);
    }

    @Test
    void restoredRunningAuctionKeepsEnforcingBidRules() {
        Auction auction = Auction.restore(id, sellerId, car(), START_PRICE, Money.of("1500"), bidderId,
                START, END, AuctionStatus.RUNNING, null, 3);

        assertThatThrownBy(() -> auction.placeBid(buyer(), Money.of("1500"), clockAt(NOW)))
                .isInstanceOf(BidTooLowException.class);
        assertThat(auction.placeBid(buyer(), Money.of("1500.01"), clockAt(NOW))).isNotNull();
    }

    @Test
    void stillEnforcesStructuralInvariants() {
        assertThatThrownBy(() -> Auction.restore(id, sellerId, car(), START_PRICE, null, null,
                END, START, AuctionStatus.RUNNING, null, 0))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("endDate");
    }

    @Test
    void highestBidAndHighestBidderGoTogether() {
        assertThatThrownBy(() -> Auction.restore(id, sellerId, car(), START_PRICE, Money.of("1500"), null,
                START, END, AuctionStatus.RUNNING, null, 0))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("highestBidderId");
        assertThatThrownBy(() -> Auction.restore(id, sellerId, car(), START_PRICE, null, bidderId,
                START, END, AuctionStatus.RUNNING, null, 0))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("highestBidderId");
    }

    @Test
    void onlyAnEndedAuctionCanHaveAWinner() {
        assertThatThrownBy(() -> Auction.restore(id, sellerId, car(), START_PRICE, Money.of("1500"), bidderId,
                START, END, AuctionStatus.RUNNING, bidderId, 0))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("wonByUserId");
    }
}
