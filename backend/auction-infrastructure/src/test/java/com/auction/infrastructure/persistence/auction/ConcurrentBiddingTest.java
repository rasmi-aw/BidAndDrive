package com.auction.infrastructure.persistence.auction;

import com.auction.application.command.PlaceBidCommand;
import com.auction.application.command.PlaceBidUseCase;
import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionConcurrentlyModifiedException;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidRepository;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import com.auction.domain.user.UserRepository;
import com.auction.infrastructure.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.auction.infrastructure.PersistenceFixtures.CLOCK;
import static com.auction.infrastructure.PersistenceFixtures.newBuyer;
import static com.auction.infrastructure.PersistenceFixtures.newRunningAuction;
import static com.auction.infrastructure.PersistenceFixtures.newSeller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Two buyers bid on the same car at the same moment: optimistic locking must let exactly one bid win.
 */
class ConcurrentBiddingTest extends PostgresIntegrationTest {

    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void twoSimultaneousBidsOnlyOneWins() throws Exception {
        User seller = userRepository.save(newSeller());
        User alice = userRepository.save(newBuyer());
        User bob = userRepository.save(newBuyer());
        AuctionId auctionId = auctionRepository.save(newRunningAuction(seller)).getId();

        // both transactions must have read version 0 before either one writes
        CyclicBarrier bothLoaded = new CyclicBarrier(2);
        PlaceBidUseCase placeBid = new PlaceBidUseCase(
                new WaitForEachOtherAfterLoading(auctionRepository, bothLoaded), userRepository, bidRepository, CLOCK);

        Future<BidId> aliceBid = executor.submit(() -> inTransaction(placeBid, auctionId, alice, "1200"));
        Future<BidId> bobBid = executor.submit(() -> inTransaction(placeBid, auctionId, bob, "1300"));

        List<Outcome> outcomes = List.of(outcome(aliceBid, alice, "1200"), outcome(bobBid, bob, "1300"));

        assertThat(outcomes).filteredOn(Outcome::won).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !o.won()).singleElement()
                .extracting(Outcome::failure).isInstanceOf(AuctionConcurrentlyModifiedException.class);

        Outcome winner = outcomes.stream().filter(Outcome::won).findFirst().orElseThrow();
        Auction stored = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(1);
        assertThat(stored.getCurrentHighestBid()).contains(winner.amount());
        assertThat(stored.getHighestBidderId()).contains(winner.bidder().getId());

        List<Map<String, Object>> bids = jdbcTemplate.queryForList("SELECT user_id FROM bids WHERE auction_id = ?",
                auctionId.value());
        assertThat(bids).singleElement().extracting(row -> row.get("user_id")).isEqualTo(winner.bidder().getId().value());
    }

    private BidId inTransaction(PlaceBidUseCase placeBid, AuctionId auctionId, User bidder, String amount) {
        return transactionTemplate.execute(status ->
                placeBid.handle(new PlaceBidCommand(auctionId, bidder.getId(), Money.of(amount))));
    }

    private static Outcome outcome(Future<BidId> future, User bidder, String amount)
            throws InterruptedException, TimeoutException {
        Throwable failure = catchThrowable(() -> future.get(30, TimeUnit.SECONDS));
        if (failure instanceof TimeoutException timeout) {
            throw timeout;
        }
        return new Outcome(bidder, Money.of(amount), failure instanceof ExecutionException e ? e.getCause() : failure);
    }

    private record Outcome(User bidder, Money amount, Throwable failure) {
        boolean won() {
            return failure == null;
        }
    }

    /**
     * Test-only decorator: after loading, each caller waits for the other one, forcing the race to happen.
     */
    private record WaitForEachOtherAfterLoading(AuctionRepository delegate, CyclicBarrier barrier)
            implements AuctionRepository {

        @Override
        public Auction save(Auction auction) {
            return delegate.save(auction);
        }

        @Override
        public Optional<Auction> findById(AuctionId id) {
            Optional<Auction> loaded = delegate.findById(id);
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("the two bidders did not load the auction concurrently", e);
            }
            return loaded;
        }
    }
}
