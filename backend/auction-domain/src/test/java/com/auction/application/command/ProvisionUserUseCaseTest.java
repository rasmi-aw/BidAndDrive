package com.auction.application.command;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.auction.domain.DomainFixtures.NOW;
import static com.auction.domain.DomainFixtures.clockAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvisionUserUseCaseTest {

    private static final Email EMAIL = new Email("jane@example.com");
    private static final Phone PHONE = new Phone("+33612345678");

    @Mock
    private UserRepository userRepository;

    private ProvisionUserUseCase useCase;

    private final UserId id = UserId.newId();

    @BeforeEach
    void setUp() {
        useCase = new ProvisionUserUseCase(userRepository, clockAt(NOW));
    }

    @Test
    void registersAnUnknownUserWithTheIdentityProviderId() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        when(userRepository.save(any())).then(returnsFirstArg());

        UserId result = useCase.handle(new ProvisionUserCommand(id, "Jane Doe", EMAIL, PHONE, Role.SELLER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(result).isEqualTo(id);
        assertThat(saved.getValue().getId()).isEqualTo(id);
        assertThat(saved.getValue().getRole()).isEqualTo(Role.SELLER);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void knownUserWithTheSameProfileIsNotSavedAgain() {
        User existing = User.register(id, "Jane Doe", EMAIL, PHONE, Role.BUYER, clockAt(NOW));
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThat(useCase.handle(new ProvisionUserCommand(id, "Jane Doe", EMAIL, PHONE, Role.BUYER))).isEqualTo(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void knownUserWhoseProfileChangedIsUpdated() {
        User existing = User.register(id, "Jane Doe", EMAIL, PHONE, Role.BUYER, clockAt(NOW.minusSeconds(60)));
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).then(returnsFirstArg());

        useCase.handle(new ProvisionUserCommand(id, "Jane Doe", EMAIL, PHONE, Role.SELLER));

        verify(userRepository).save(existing);
        assertThat(existing.getRole()).isEqualTo(Role.SELLER);
        assertThat(existing.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void commandRequiresUserId() {
        assertThatThrownBy(() -> new ProvisionUserCommand(null, "Jane Doe", EMAIL, PHONE, Role.BUYER))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("userId");
    }
}
