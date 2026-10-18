package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidReadRepository;
import com.auction.domain.auction.BidRepository;
import com.auction.domain.shared.Money;
import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;
import com.auction.domain.user.User;
import com.auction.domain.user.UserRepository;
import com.auction.infrastructure.InfrastructureIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;

import static com.auction.infrastructure.PersistenceFixtures.NOW;
import static com.auction.infrastructure.PersistenceFixtures.newBuyer;
import static com.auction.infrastructure.PersistenceFixtures.newRunningAuction;
import static com.auction.infrastructure.PersistenceFixtures.newSeller;
import static org.assertj.core.api.Assertions.assertThat;

class BidReadPersistenceAdapterTest extends InfrastructureIntegrationTest {

    @Autowired
    private BidReadRepository bidReadRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserRepository userRepository;

    private Auction auction;
    private Bid first;
    private Bid second;
    private Bid third;

    @BeforeEach
    void bids() {
        User seller = userRepository.save(newSeller());
        User buyer = userRepository.save(newBuyer());
        auction = auctionRepository.save(newRunningAuction(seller));
        first = bid(buyer, "1100", 1);
        second = bid(buyer, "1200", 2);
        third = bid(buyer, "1300", 3);

        Auction other = auctionRepository.save(newRunningAuction(seller));
        bidRepository.save(other.placeBid(buyer, Money.of("5000"), clockAt(1)));
    }

    @Test
    void returnsTheAuctionsBidsNewestFirst() {
        Page<Bid> page = bidReadRepository.findByAuctionId(auction.getId(), new PageQuery(0, 10));

        assertThat(page.content()).containsExactly(third, second, first);
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void pagesThroughBids() {
        Page<Bid> secondPage = bidReadRepository.findByAuctionId(auction.getId(), new PageQuery(1, 2));

        assertThat(secondPage.content()).containsExactly(first);
        assertThat(secondPage.totalPages()).isEqualTo(2);
    }

    @Test
    void softDeletedBidsAreHidden() {
        jdbcTemplate.update("UPDATE bids SET deleted = TRUE WHERE id = ?", second.id().value());

        assertThat(bidReadRepository.findByAuctionId(auction.getId(), new PageQuery(0, 10)).content())
                .containsExactly(third, first);
    }

    private Bid bid(User buyer, String amount, int minutesFromNow) {
        Bid bid = auction.placeBid(buyer, Money.of(amount), clockAt(minutesFromNow));
        auction = auctionRepository.save(auction);
        return bidRepository.save(bid);
    }

    private static Clock clockAt(int minutesFromNow) {
        return Clock.fixed(NOW.plus(Duration.ofMinutes(minutesFromNow)), ZoneOffset.UTC);
    }
}
