package com.auction.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizesScaleSoEqualAmountsAreEqual() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of("10").amount()).isEqualByComparingTo("10.00");
        assertThat(Money.of("10").amount().scale()).isEqualTo(2);
    }

    @Test
    void acceptsZero() {
        assertThat(Money.of("0").isZero()).isTrue();
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Money(null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.of("-0.01"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.001", "0.999"})
    void rejectsMoreThanTwoDecimals(String amount) {
        assertThatThrownBy(() -> Money.of(amount))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void comparesAmounts() {
        assertThat(Money.of("10.01").isGreaterThan(Money.of("10"))).isTrue();
        assertThat(Money.of("10").isGreaterThan(Money.of("10.00"))).isFalse();
        assertThat(Money.of("9.99").isGreaterThan(Money.of("10"))).isFalse();
    }

    @Test
    void maxReturnsTheLargerAmount() {
        assertThat(Money.of("5").max(Money.of("7"))).isEqualTo(Money.of("7"));
        assertThat(Money.of("7").max(Money.of("5"))).isEqualTo(Money.of("7"));
    }

    @Test
    void domainExceptionsShareACommonBaseType() {
        assertThat(new DomainValidationException("x")).isInstanceOf(DomainException.class);
    }
}
