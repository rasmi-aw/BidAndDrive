package com.auction.application.query;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.List;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.START;
import static com.auction.domain.DomainFixtures.clockAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRunningAuctionsUseCaseTest {

    @Mock
    private AuctionReadRepository auctionReadRepository;

    private ListRunningAuctionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListRunningAuctionsUseCase(auctionReadRepository, clockAt(NOW));
    }

    @Test
    void returnsTheRequestedPageOfRunningAuctionsAsOfNow() {
        PageQuery pageQuery = new PageQuery(1, 2);
        Page<AuctionSummary> expected = new Page<>(List.of(summary(), summary()), 1, 2, 5);
        when(auctionReadRepository.findRunning(NOW, pageQuery)).thenReturn(expected);

        Page<AuctionSummary> result = useCase.handle(ListRunningAuctionsQuery.of(1, 2));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void invalidPagingIsRejectedBeforeQuerying() {
        assertThatThrownBy(() -> useCase.handle(ListRunningAuctionsQuery.of(0, 500)))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(auctionReadRepository);
    }

    @Test
    void queryRequiresPageQuery() {
        assertThatThrownBy(() -> new ListRunningAuctionsQuery(null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("pageQuery");
    }

    private static AuctionSummary summary() {
        return new AuctionSummary(AuctionId.newId(), UserId.newId(), "Peugeot 308", CarCondition.GOOD,
                Year.of(2018), Year.of(2019), Money.of("1000"), null, START, END);
    }
}
