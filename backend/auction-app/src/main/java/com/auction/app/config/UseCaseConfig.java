package com.auction.app.config;

import com.auction.application.command.CloseAuctionUseCase;
import com.auction.application.command.CreateAuctionUseCase;
import com.auction.application.command.PlaceBidUseCase;
import com.auction.application.command.ProvisionUserUseCase;
import com.auction.application.query.GetAuctionUseCase;
import com.auction.application.query.ListAuctionBidsUseCase;
import com.auction.application.query.ListRunningAuctionsUseCase;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.BidReadRepository;
import com.auction.domain.auction.BidRepository;
import com.auction.domain.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

import static com.auction.app.config.TransactionalProxies.readOnly;
import static com.auction.app.config.TransactionalProxies.readWrite;

/**
 * Wires use cases to the persistence adapters. Commands run in a read-write transaction, queries read-only.
 */
@Configuration
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateAuctionUseCase createAuctionUseCase(UserRepository users, AuctionRepository auctions,
                                              PlatformTransactionManager tx) {
        return readWrite(new CreateAuctionUseCase(users, auctions), tx);
    }

    @Bean
    PlaceBidUseCase placeBidUseCase(AuctionRepository auctions, UserRepository users, BidRepository bids,
                                    Clock clock, PlatformTransactionManager tx) {
        return readWrite(new PlaceBidUseCase(auctions, users, bids, clock), tx);
    }

    @Bean
    CloseAuctionUseCase closeAuctionUseCase(AuctionRepository auctions, PlatformTransactionManager tx) {
        return readWrite(new CloseAuctionUseCase(auctions), tx);
    }

    @Bean
    ProvisionUserUseCase provisionUserUseCase(UserRepository users, Clock clock, PlatformTransactionManager tx) {
        return readWrite(new ProvisionUserUseCase(users, clock), tx);
    }

    @Bean
    ListRunningAuctionsUseCase listRunningAuctionsUseCase(AuctionReadRepository auctions, Clock clock,
                                                          PlatformTransactionManager tx) {
        return readOnly(new ListRunningAuctionsUseCase(auctions, clock), tx);
    }

    @Bean
    GetAuctionUseCase getAuctionUseCase(AuctionReadRepository auctions, PlatformTransactionManager tx) {
        return readOnly(new GetAuctionUseCase(auctions), tx);
    }

    @Bean
    ListAuctionBidsUseCase listAuctionBidsUseCase(AuctionReadRepository auctions, BidReadRepository bids,
                                                  PlatformTransactionManager tx) {
        return readOnly(new ListAuctionBidsUseCase(auctions, bids), tx);
    }
}
