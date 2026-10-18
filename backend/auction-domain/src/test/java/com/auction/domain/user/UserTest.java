package com.auction.domain.user;

import com.auction.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final Email EMAIL = new Email("jane@example.com");
    private static final Phone PHONE = new Phone("+33612345678");

    @Test
    void registersUserWithGeneratedIdAndTimestampsFromClock() {
        User user = User.register("  Jane Doe ", EMAIL, PHONE, Role.SELLER, CLOCK);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getFullName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getPhone()).isEqualTo(PHONE);
        assertThat(user.getRole()).isEqualTo(Role.SELLER);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void eachRegistrationGetsItsOwnId() {
        User first = User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK);
        User second = User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK);

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void exposesRoleChecks() {
        assertThat(User.register("Jane Doe", EMAIL, PHONE, Role.SELLER, CLOCK).isSeller()).isTrue();
        assertThat(User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK).isBuyer()).isTrue();
        assertThat(User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK).isSeller()).isFalse();
    }

    @Test
    void registersWithAnIdAssignedByTheIdentityProvider() {
        UserId id = UserId.newId();

        User user = User.register(id, "Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void updatingTheProfileChangesFieldsAndTouchesUpdatedAt() {
        User user = User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK);
        Instant later = NOW.plusSeconds(3600);

        boolean changed = user.updateProfile("Jane Smith", new Email("jane.smith@example.com"),
                new Phone("+33699999999"), Role.SELLER, Clock.fixed(later, ZoneOffset.UTC));

        assertThat(changed).isTrue();
        assertThat(user.getFullName()).isEqualTo("Jane Smith");
        assertThat(user.getEmail()).isEqualTo(new Email("jane.smith@example.com"));
        assertThat(user.getPhone()).isEqualTo(new Phone("+33699999999"));
        assertThat(user.getRole()).isEqualTo(Role.SELLER);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void updatingWithTheSameProfileChangesNothing() {
        User user = User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK);

        boolean changed = user.updateProfile(" Jane Doe ", EMAIL, PHONE, Role.BUYER,
                Clock.fixed(NOW.plusSeconds(3600), ZoneOffset.UTC));

        assertThat(changed).isFalse();
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void updatingTheProfileValidatesLikeRegistration() {
        User user = User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, CLOCK);

        assertThatThrownBy(() -> user.updateProfile(" ", EMAIL, PHONE, Role.BUYER, CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("fullName");
        assertThatThrownBy(() -> user.updateProfile("Jane Doe", EMAIL, PHONE, null, CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("role");
        assertThat(user.getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    void restoresAStoredUserAsIs() {
        UserId id = UserId.newId();
        Instant updatedAt = NOW.plusSeconds(60);

        User user = User.restore(id, "Jane Doe", EMAIL, PHONE, Role.BUYER, NOW, updatedAt);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getFullName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getPhone()).isEqualTo(PHONE);
        assertThat(user.getRole()).isEqualTo(Role.BUYER);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void restoreStillRequiresEveryField() {
        assertThatThrownBy(() -> User.restore(null, "Jane Doe", EMAIL, PHONE, Role.BUYER, NOW, NOW))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> User.restore(UserId.newId(), "Jane Doe", EMAIL, PHONE, Role.BUYER, NOW, null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("updatedAt");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void requiresFullName(String fullName) {
        assertThatThrownBy(() -> User.register(fullName, EMAIL, PHONE, Role.BUYER, CLOCK))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("fullName");
    }

    @Test
    void requiresEmailPhoneRoleAndClock() {
        assertThatThrownBy(() -> User.register("Jane Doe", null, PHONE, Role.BUYER, CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("email");
        assertThatThrownBy(() -> User.register("Jane Doe", EMAIL, null, Role.BUYER, CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("phone");
        assertThatThrownBy(() -> User.register("Jane Doe", EMAIL, PHONE, null, CLOCK))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("role");
        assertThatThrownBy(() -> User.register("Jane Doe", EMAIL, PHONE, Role.BUYER, null))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("clock");
    }
}
