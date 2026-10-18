package com.auction.domain.auction;

import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Year;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.START;
import static org.assertj.core.api.Assertions.assertThat;

class AuctionSummaryTest {

    @Test
    void runningAuctionIsOpenForBiddingInsideItsWindow() {
        AuctionSummary summary = summary(AuctionStatus.RUNNING);

        assertThat(summary.isOpenForBidding(START)).isTrue();
        assertThat(summary.isOpenForBidding(NOW)).isTrue();
        assertThat(summary.isOpenForBidding(START.minus(Duration.ofSeconds(1)))).isFalse();
        assertThat(summary.isOpenForBidding(END)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"CANCELLED", "ENDED"})
    void closedAuctionIsNeverOpenForBidding(AuctionStatus status) {
        assertThat(summary(status).isOpenForBidding(NOW)).isFalse();
    }

    private static AuctionSummary summary(AuctionStatus status) {
        return new AuctionSummary(AuctionId.newId(), UserId.newId(), "Peugeot 308", CarCondition.GOOD,
                Year.of(2018), Year.of(2019), "One owner", Money.of("1000"), null, START, END, status);
    }
}
