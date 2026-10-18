package com.auction.domain.auction;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.START;
import static com.auction.domain.DomainFixtures.START_PRICE;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.car;
import static com.auction.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionCreationTest {

    @Test
    void sellerCreatesARunningAuctionWithoutBids() {
        User seller = seller();

        Auction auction = Auction.create(seller, car(), START_PRICE, START, END);

        assertThat(auction.getId()).isNotNull();
        assertThat(auction.getSellerId()).isEqualTo(seller.getId());
        assertThat(auction.getCar()).isEqualTo(car());
        assertThat(auction.getStartPrice()).isEqualTo(START_PRICE);
        assertThat(auction.getStartDate()).isEqualTo(START);
        assertThat(auction.getEndDate()).isEqualTo(END);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(auction.getCurrentHighestBid()).isEmpty();
        assertThat(auction.getWonByUserId()).isEmpty();
        assertThat(auction.getVersion()).isZero();
    }

    @Test
    void buyerCannotCreateAnAuction() {
        assertThatThrownBy(() -> Auction.create(buyer(), car(), START_PRICE, START, END))
                .isInstanceOf(OnlySellerCanCreateAuctionException.class);
    }

    @Test
    void endDateMustBeAfterStartDate() {
        assertThatThrownBy(() -> Auction.create(seller(), car(), START_PRICE, START, START))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("endDate");
        assertThatThrownBy(() -> Auction.create(seller(), car(), START_PRICE, END, START))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("endDate");
    }

    @Test
    void startPriceMustBeStrictlyPositive() {
        assertThatThrownBy(() -> Auction.create(seller(), car(), Money.of("0"), START, END))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("startPrice");
    }

    @Test
    void requiresAllArguments() {
        assertThatThrownBy(() -> Auction.create(null, car(), START_PRICE, START, END))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("seller");
        assertThatThrownBy(() -> Auction.create(seller(), null, START_PRICE, START, END))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("car");
        assertThatThrownBy(() -> Auction.create(seller(), car(), null, START, END))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("startPrice");
        assertThatThrownBy(() -> Auction.create(seller(), car(), START_PRICE, null, END))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("startDate");
        assertThatThrownBy(() -> Auction.create(seller(), car(), START_PRICE, START, null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("endDate");
    }
}
