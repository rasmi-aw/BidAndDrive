package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionNotRunningException;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.shared.DomainEventPublisher;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.clockAt;
import static com.auction.domain.DomainFixtures.runningAuction;
import static com.auction.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseAuctionUseCaseTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    private CloseAuctionUseCase useCase;

    private final Auction auction = runningAuction(seller());

    @BeforeEach
    void setUp() {
        useCase = new CloseAuctionUseCase(auctionRepository, eventPublisher, clockAt(NOW));
    }

    @Test
    void closesTheAuctionAwardsItToTheHighestBidderAndSavesIt() {
        User buyer = buyer();
        auction.placeBid(buyer, Money.of("1500"), clockAt(NOW));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(auctionRepository.save(auction)).then(returnsFirstArg());

        auction.pullEvents();

        useCase.handle(new CloseAuctionCommand(auction.getId()));

        InOrder order = inOrder(auctionRepository, eventPublisher);
        order.verify(auctionRepository).save(auction);
        order.verify(eventPublisher).publish(new AuctionClosed(auction.getId(), buyer.getId(), Money.of("1500"), NOW));
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(auction.getWonByUserId()).contains(buyer.getId());
    }

    @Test
    void unknownAuctionIsRejected() {
        AuctionId unknown = AuctionId.newId();
        when(auctionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new CloseAuctionCommand(unknown)))
                .isInstanceOf(AuctionNotFoundException.class);
        verify(auctionRepository, never()).save(any());
    }

    @Test
    void alreadyEndedAuctionIsRejectedAndNotSavedAgain() {
        auction.close(clockAt(NOW));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> useCase.handle(new CloseAuctionCommand(auction.getId())))
                .isInstanceOf(AuctionNotRunningException.class);
        verify(auctionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void commandRequiresAuctionId() {
        assertThatThrownBy(() -> new CloseAuctionCommand(null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("auctionId");
    }
}
