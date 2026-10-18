package com.auction.domain.user;

import com.auction.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesToTrimmedLowerCase() {
        assertThat(new Email("  John.Doe@Example.COM ").value()).isEqualTo("john.doe@example.com");
    }

    @Test
    void equalWhenSameAddressIgnoringCase() {
        assertThat(new Email("a@b.io")).isEqualTo(new Email("A@B.IO"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "plainaddress", "@no-local.com", "no-at.com", "a@b", "a b@c.com", "a@@b.com"})
    void rejectsInvalidAddresses(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(DomainValidationException.class);
    }
}
