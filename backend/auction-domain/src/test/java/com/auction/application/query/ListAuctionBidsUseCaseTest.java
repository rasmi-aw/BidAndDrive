package com.auction.application.query;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidReadRepository;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;
import com.auction.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAuctionBidsUseCaseTest {

    @Mock
    private AuctionReadRepository auctionReadRepository;
    @Mock
    private BidReadRepository bidReadRepository;
    @InjectMocks
    private ListAuctionBidsUseCase useCase;

    private final AuctionId auctionId = AuctionId.newId();

    @Test
    void returnsThePageOfBidsOfAnExistingAuction() {
        when(auctionReadRepository.findById(auctionId)).thenReturn(Optional.of(summary()));
        Bid bid = new Bid(BidId.newId(), auctionId, UserId.newId(), Money.of("1200"), NOW);
        Page<Bid> expected = new Page<>(List.of(bid), 0, 20, 1);
        when(bidReadRepository.findByAuctionId(auctionId, new PageQuery(0, 20))).thenReturn(expected);

        assertThat(useCase.handle(ListAuctionBidsQuery.of(auctionId, 0, 20))).isEqualTo(expected);
    }

    @Test
    void unknownAuctionIsNotFoundAndBidsAreNotQueried() {
        when(auctionReadRepository.findById(auctionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ListAuctionBidsQuery.of(auctionId, 0, 20)))
                .isInstanceOf(AuctionNotFoundException.class);
        verifyNoInteractions(bidReadRepository);
    }

    @Test
    void queryRequiresAuctionIdAndValidPaging() {
        assertThatThrownBy(() -> ListAuctionBidsQuery.of(null, 0, 20))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("auctionId");
        assertThatThrownBy(() -> ListAuctionBidsQuery.of(auctionId, -1, 20))
                .isInstanceOf(DomainValidationException.class);
    }

    private AuctionSummary summary() {
        return new AuctionSummary(auctionId, UserId.newId(), "Peugeot 308", CarCondition.GOOD,
                Year.of(2018), Year.of(2019), "One owner", Money.of("1000"), null, START, END, AuctionStatus.RUNNING);
    }
}
