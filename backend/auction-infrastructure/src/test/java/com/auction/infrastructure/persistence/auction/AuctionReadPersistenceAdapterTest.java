package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.shared.Money;
import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;
import com.auction.domain.user.User;
import com.auction.domain.user.UserRepository;
import com.auction.infrastructure.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static com.auction.infrastructure.PersistenceFixtures.CLOCK;
import static com.auction.infrastructure.PersistenceFixtures.NOW;
import static com.auction.infrastructure.PersistenceFixtures.newAuction;
import static com.auction.infrastructure.PersistenceFixtures.newBuyer;
import static com.auction.infrastructure.PersistenceFixtures.newSeller;
import static org.assertj.core.api.Assertions.assertThat;

class AuctionReadPersistenceAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private AuctionReadRepository auctionReadRepository;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserRepository userRepository;

    private User seller;
    private Auction endsFirst;
    private Auction endsSecond;
    private Auction endsThird;

    @BeforeEach
    void auctions() {
        seller = userRepository.save(newSeller());
        User buyer = userRepository.save(newBuyer());

        endsThird = save(newAuction(seller, NOW.minus(hours(1)), NOW.plus(hours(30))));
        endsFirst = save(newAuction(seller, NOW.minus(hours(1)), NOW.plus(hours(10))));
        endsSecond = newAuction(seller, NOW.minus(hours(1)), NOW.plus(hours(20)));
        endsSecond.placeBid(buyer, Money.of("1250"), CLOCK);
        save(endsSecond);

        save(newAuction(seller, NOW.plus(hours(1)), NOW.plus(hours(5))));      // not started yet
        save(newAuction(seller, NOW.minus(hours(5)), NOW.minus(hours(1))));    // window over, not closed yet
        Auction cancelled = newAuction(seller, NOW.minus(hours(1)), NOW.plus(hours(5)));
        cancelled.cancel();
        save(cancelled);
        Auction ended = newAuction(seller, NOW.minus(hours(1)), NOW.plus(hours(5)));
        ended.close();
        save(ended);
        Auction softDeleted = save(newAuction(seller, NOW.minus(hours(1)), NOW.plus(hours(5))));
        jdbcTemplate.update("UPDATE auctions SET deleted = TRUE WHERE id = ?", softDeleted.getId().value());
    }

    @Test
    void returnsOnlyAuctionsOpenForBiddingOrderedByEndDate() {
        Page<AuctionSummary> page = auctionReadRepository.findRunning(NOW, new PageQuery(0, 10));

        assertThat(page.content()).extracting(AuctionSummary::id)
                .containsExactly(endsFirst.getId(), endsSecond.getId(), endsThird.getId());
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void pagesThroughResults() {
        Page<AuctionSummary> first = auctionReadRepository.findRunning(NOW, new PageQuery(0, 2));
        Page<AuctionSummary> second = auctionReadRepository.findRunning(NOW, new PageQuery(1, 2));

        assertThat(first.content()).extracting(AuctionSummary::id).containsExactly(endsFirst.getId(), endsSecond.getId());
        assertThat(second.content()).extracting(AuctionSummary::id).containsExactly(endsThird.getId());
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(2);
        assertThat(second.totalElements()).isEqualTo(3);
        assertThat(second.totalPages()).isEqualTo(2);
    }

    @Test
    void mapsSummaryFields() {
        AuctionSummary summary = auctionReadRepository.findRunning(NOW, new PageQuery(0, 10)).content().get(1);

        assertThat(summary.id()).isEqualTo(endsSecond.getId());
        assertThat(summary.sellerId()).isEqualTo(seller.getId());
        assertThat(summary.carName()).isEqualTo("Peugeot 308");
        assertThat(summary.startPrice()).isEqualTo(Money.of("1000"));
        assertThat(summary.currentHighestBid()).isEqualTo(Money.of("1250"));
        assertThat(summary.endDate()).isEqualTo(endsSecond.getEndDate());
    }

    private Auction save(Auction auction) {
        return auctionRepository.save(auction);
    }

    private static Duration hours(long hours) {
        return Duration.ofHours(hours);
    }

}
