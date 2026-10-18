package com.auction.domain.auction;

import com.auction.domain.shared.DomainEvent;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.START;
import static com.auction.domain.DomainFixtures.START_PRICE;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.car;
import static com.auction.domain.DomainFixtures.clockAt;
import static com.auction.domain.DomainFixtures.runningAuction;
import static com.auction.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionEventsTest {

    private static final Clock CLOCK = clockAt(NOW);

    private final User buyer = buyer();
    private final Auction auction = runningAuction(seller());

    @Test
    void placingABidRaisesBidPlaced() {
        Bid bid = auction.placeBid(buyer, Money.of("1200"), CLOCK);

        assertThat(auction.pullEvents()).containsExactly(
                new BidPlaced(auction.getId(), bid.id(), buyer.getId(), Money.of("1200"), NOW));
    }

    @Test
    void aRejectedBidRaisesNothing() {
        assertThatThrownBy(() -> auction.placeBid(buyer, Money.of("10"), CLOCK))
                .isInstanceOf(BidTooLowException.class);

        assertThat(auction.pullEvents()).isEmpty();
    }

    @Test
    void closingRaisesAuctionClosedWithTheWinnerAndTheWinningBid() {
        auction.placeBid(buyer, Money.of("1500"), CLOCK);
        auction.pullEvents();
        Clock later = clockAt(NOW.plus(Duration.ofHours(1)));

        auction.close(later);

        AuctionClosed closed = (AuctionClosed) auction.pullEvents().getFirst();
        assertThat(closed.auctionId()).isEqualTo(auction.getId());
        assertThat(closed.winner()).contains(buyer.getId());
        assertThat(closed.winningBid()).contains(Money.of("1500"));
        assertThat(closed.occurredAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void closingWithoutBidsRaisesAuctionClosedWithoutWinner() {
        auction.close(CLOCK);

        AuctionClosed closed = (AuctionClosed) auction.pullEvents().getFirst();
        assertThat(closed.winner()).isEmpty();
        assertThat(closed.winningBid()).isEmpty();
    }

    @Test
    void pullingReturnsEventsInOrderOnceAndClearsThem() {
        auction.placeBid(buyer, Money.of("1200"), CLOCK);
        auction.close(CLOCK);

        assertThat(auction.pullEvents()).extracting(DomainEvent::getClass)
                .containsExactly(BidPlaced.class, AuctionClosed.class);
        assertThat(auction.pullEvents()).isEmpty();
    }

    @Test
    void creatingCancellingOrRestoringRaisesNothing() {
        Auction created = Auction.create(seller(), car(), START_PRICE, START, END);
        created.cancel();
        Auction restored = Auction.restore(AuctionId.newId(), seller().getId(), car(), START_PRICE, null, null,
                START, END, AuctionStatus.RUNNING, null, 3);

        assertThat(created.pullEvents()).isEmpty();
        assertThat(restored.pullEvents()).isEmpty();
    }
}
