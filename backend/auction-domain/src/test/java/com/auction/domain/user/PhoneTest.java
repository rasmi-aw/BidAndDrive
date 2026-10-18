package com.auction.domain.user;

import com.auction.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneTest {

    @Test
    void stripsFormattingCharacters() {
        assertThat(new Phone("+33 6 12-34.56 (78)").value()).isEqualTo("+33612345678");
    }

    @Test
    void acceptsNumberWithoutCountryPrefix() {
        assertThat(new Phone("0612345678").value()).isEqualTo("0612345678");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "1234567", "+1234567890123456", "06-12-AB-56-78", "++33612345678", "33+612345678"})
    void rejectsInvalidNumbers(String value) {
        assertThatThrownBy(() -> new Phone(value))
                .isInstanceOf(DomainValidationException.class);
    }
}
