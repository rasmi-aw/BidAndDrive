package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionConcurrentlyModifiedException;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidRepository;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import com.auction.domain.user.UserRepository;
import com.auction.infrastructure.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static com.auction.infrastructure.PersistenceFixtures.CLOCK;
import static com.auction.infrastructure.PersistenceFixtures.newBuyer;
import static com.auction.infrastructure.PersistenceFixtures.newRunningAuction;
import static com.auction.infrastructure.PersistenceFixtures.newSeller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionPersistenceAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private User seller;
    private User buyer;

    @BeforeEach
    void users() {
        seller = userRepository.save(newSeller());
        buyer = userRepository.save(newBuyer());
    }

    @Test
    void savesANewAuctionAndLoadsItBackAtVersionZero() {
        Auction auction = newRunningAuction(seller);

        auctionRepository.save(auction);

        Auction loaded = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(loaded).usingRecursiveComparison().isEqualTo(auction);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    void savingChangesIncrementsTheVersion() {
        Auction auction = auctionRepository.save(newRunningAuction(seller));
        Auction loaded = auctionRepository.findById(auction.getId()).orElseThrow();

        loaded.placeBid(buyer, Money.of("1500"), CLOCK);
        Auction saved = auctionRepository.save(loaded);

        assertThat(saved.getVersion()).isEqualTo(1);
        Auction reloaded = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(reloaded.getCurrentHighestBid()).contains(Money.of("1500"));
        assertThat(reloaded.getHighestBidderId()).contains(buyer.getId());
        assertThat(reloaded.getVersion()).isEqualTo(1);
    }

    @Test
    void staleCopyCannotOverwriteANewerVersion() {
        Auction auction = auctionRepository.save(newRunningAuction(seller));
        Auction first = auctionRepository.findById(auction.getId()).orElseThrow();
        Auction stale = auctionRepository.findById(auction.getId()).orElseThrow();
        first.placeBid(buyer, Money.of("1500"), CLOCK);
        auctionRepository.save(first);

        stale.placeBid(userRepository.save(newBuyer()), Money.of("1300"), CLOCK);

        assertThatThrownBy(() -> auctionRepository.save(stale))
                .isInstanceOf(AuctionConcurrentlyModifiedException.class);
        Auction stored = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(stored.getCurrentHighestBid()).contains(Money.of("1500"));
        assertThat(stored.getVersion()).isEqualTo(1);
    }

    @Test
    void persistsClosingState() {
        Auction auction = auctionRepository.save(newRunningAuction(seller));
        Auction loaded = auctionRepository.findById(auction.getId()).orElseThrow();
        loaded.placeBid(buyer, Money.of("1500"), CLOCK);
        loaded.close(CLOCK);

        auctionRepository.save(loaded);

        Auction reloaded = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(reloaded.getWonByUserId()).contains(buyer.getId());
    }

    @Test
    void unknownAuctionIsEmpty() {
        assertThat(auctionRepository.findById(AuctionId.newId())).isEmpty();
    }

    @Test
    void savesBids() {
        Auction auction = auctionRepository.save(newRunningAuction(seller));
        Bid bid = auction.placeBid(buyer, Money.of("1500"), CLOCK);

        bidRepository.save(bid);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM bids WHERE id = ?", bid.id().value());
        assertThat(row.get("auction_id")).isEqualTo(auction.getId().value());
        assertThat(row.get("user_id")).isEqualTo(buyer.getId().value());
        assertThat(row.get("deleted")).isEqualTo(false);
    }

    @Test
    void softDeletedAuctionIsNotFound() {
        Auction auction = auctionRepository.save(newRunningAuction(seller));

        jdbcTemplate.update("UPDATE auctions SET deleted = TRUE WHERE id = ?", auction.getId().value());

        assertThat(auctionRepository.findById(auction.getId())).isEmpty();
    }

    @Test
    void removingAnAuctionEntityOnlyFlagsItAsDeleted() {
        Auction auction = auctionRepository.save(newRunningAuction(seller));

        transactionTemplate.executeWithoutResult(status ->
                entityManager.remove(entityManager.find(AuctionEntity.class, auction.getId().value())));

        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM auctions WHERE id = ?", Boolean.class,
                auction.getId().value())).isTrue();
        assertThat(auctionRepository.findById(auction.getId())).isEmpty();
    }
}
