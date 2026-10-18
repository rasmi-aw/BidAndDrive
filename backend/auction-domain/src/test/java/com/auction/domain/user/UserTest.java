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
