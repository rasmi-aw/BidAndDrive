package com.auction.application.command;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionRepository;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.auction.OnlySellerCanCreateAuctionException;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserNotFoundException;
import com.auction.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static com.auction.domain.DomainFixtures.END;
import static com.auction.domain.DomainFixtures.START;
import static com.auction.domain.DomainFixtures.START_PRICE;
import static com.auction.domain.DomainFixtures.buyer;
import static com.auction.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAuctionUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @InjectMocks
    private CreateAuctionUseCase useCase;

    @Test
    void sellerCreatesAndSavesARunningAuction() {
        User seller = seller();
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(auctionRepository.save(any())).then(returnsFirstArg());

        AuctionId id = useCase.handle(command(seller.getId()));

        ArgumentCaptor<Auction> saved = ArgumentCaptor.forClass(Auction.class);
        verify(auctionRepository).save(saved.capture());
        Auction auction = saved.getValue();
        assertThat(id).isEqualTo(auction.getId());
        assertThat(auction.getSellerId()).isEqualTo(seller.getId());
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(auction.getCar().carName()).isEqualTo("Renault Clio");
        assertThat(auction.getCar().condition()).isEqualTo(CarCondition.EXCELLENT);
        assertThat(auction.getCar().yearMade()).isEqualTo(Year.of(2021));
        assertThat(auction.getCar().yearRegistered()).isEqualTo(Year.of(2022));
        assertThat(auction.getCar().description()).isEqualTo("Like new");
        assertThat(auction.getStartPrice()).isEqualTo(START_PRICE);
        assertThat(auction.getStartDate()).isEqualTo(START);
        assertThat(auction.getEndDate()).isEqualTo(END);
    }

    @Test
    void unknownSellerIsRejected() {
        UserId unknown = UserId.newId();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(command(unknown)))
                .isInstanceOf(UserNotFoundException.class);
        verify(auctionRepository, never()).save(any());
    }

    @Test
    void buyerCannotCreateAnAuctionAndNothingIsSaved() {
        User buyer = buyer();
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> useCase.handle(command(buyer.getId())))
                .isInstanceOf(OnlySellerCanCreateAuctionException.class);
        verify(auctionRepository, never()).save(any());
    }

    @Test
    void commandRequiresSellerId() {
        assertThatThrownBy(() -> command(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("sellerId");
    }

    private static CreateAuctionCommand command(UserId sellerId) {
        return new CreateAuctionCommand(sellerId, "Renault Clio", CarCondition.EXCELLENT, Year.of(2021), Year.of(2022),
                "Like new", START_PRICE, START, END);
    }
}
