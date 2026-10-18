package com.auction.application.query;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAuctionUseCaseTest {

    @Mock
    private AuctionReadRepository auctionReadRepository;
    @InjectMocks
    private GetAuctionUseCase useCase;

    @Test
    void returnsTheAuction() {
        AuctionId id = AuctionId.newId();
        AuctionSummary summary = new AuctionSummary(id, UserId.newId(), "Peugeot 308", CarCondition.GOOD,
                Year.of(2018), Year.of(2019), Money.of("1000"), null, START, END, AuctionStatus.RUNNING);
        when(auctionReadRepository.findById(id)).thenReturn(Optional.of(summary));

        assertThat(useCase.handle(new GetAuctionQuery(id))).isEqualTo(summary);
    }

    @Test
    void unknownAuctionIsNotFound() {
        AuctionId id = AuctionId.newId();
        when(auctionReadRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new GetAuctionQuery(id)))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void queryRequiresAuctionId() {
        assertThatThrownBy(() -> new GetAuctionQuery(null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("auctionId");
    }
}
