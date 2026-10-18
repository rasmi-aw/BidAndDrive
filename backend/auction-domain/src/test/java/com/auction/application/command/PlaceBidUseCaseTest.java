package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidRepository;
import com.auction.domain.auction.BidTooLowException;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Money;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserNotFoundException;
import com.auction.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class PlaceBidUseCaseTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BidRepository bidRepository;

    private PlaceBidUseCase useCase;

    private final User buyer = buyer();
    private final Auction auction = runningAuction(seller());

    @BeforeEach
    void setUp() {
        useCase = new PlaceBidUseCase(auctionRepository, userRepository, bidRepository, clockAt(NOW));
    }

    @Test
    void acceptedBidUpdatesTheAuctionThenStoresTheBid() {
        givenAuctionAndBuyerExist();
        when(auctionRepository.save(auction)).then(returnsFirstArg());
        when(bidRepository.save(any())).then(returnsFirstArg());

        BidId bidId = useCase.handle(new PlaceBidCommand(auction.getId(), buyer.getId(), Money.of("1200")));

        InOrder order = inOrder(auctionRepository, bidRepository);
        order.verify(auctionRepository).save(auction);
        ArgumentCaptor<Bid> savedBid = ArgumentCaptor.forClass(Bid.class);
        order.verify(bidRepository).save(savedBid.capture());

        Bid bid = savedBid.getValue();
        assertThat(bidId).isEqualTo(bid.id());
        assertThat(bid.auctionId()).isEqualTo(auction.getId());
        assertThat(bid.userId()).isEqualTo(buyer.getId());
        assertThat(bid.givenPrice()).isEqualTo(Money.of("1200"));
        assertThat(bid.createdAt()).isEqualTo(NOW);
        assertThat(auction.getCurrentHighestBid()).contains(Money.of("1200"));
    }

    @Test
    void unknownAuctionIsRejected() {
        AuctionId unknown = AuctionId.newId();
        when(auctionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new PlaceBidCommand(unknown, buyer.getId(), Money.of("1200"))))
                .isInstanceOf(AuctionNotFoundException.class);
        verify(auctionRepository, never()).save(any());
        verifyNoInteractions(bidRepository);
    }

    @Test
    void unknownBidderIsRejected() {
        UserId unknown = UserId.newId();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new PlaceBidCommand(auction.getId(), unknown, Money.of("1200"))))
                .isInstanceOf(UserNotFoundException.class);
        verify(auctionRepository, never()).save(any());
        verifyNoInteractions(bidRepository);
    }

    @Test
    void domainRejectionSavesNothing() {
        givenAuctionAndBuyerExist();

        assertThatThrownBy(() -> useCase.handle(new PlaceBidCommand(auction.getId(), buyer.getId(), Money.of("10"))))
                .isInstanceOf(BidTooLowException.class);
        verify(auctionRepository, never()).save(any());
        verifyNoInteractions(bidRepository);
    }

    @Test
    void commandRequiresAuctionAndBidderIds() {
        assertThatThrownBy(() -> new PlaceBidCommand(null, buyer.getId(), Money.of("1200")))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("auctionId");
        assertThatThrownBy(() -> new PlaceBidCommand(auction.getId(), null, Money.of("1200")))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("bidderId");
    }

    private void givenAuctionAndBuyerExist() {
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
    }
}
