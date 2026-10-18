package com.auction.domain.auction;

import com.auction.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarDetailsTest {

    @Test
    void acceptsRegistrationInTheSameYearAsManufacture() {
        CarDetails car = new CarDetails(" Clio ", CarCondition.EXCELLENT, Year.of(2020), Year.of(2020), "desc");

        assertThat(car.carName()).isEqualTo("Clio");
        assertThat(car.yearRegistered()).isEqualTo(Year.of(2020));
    }

    @Test
    void rejectsRegistrationBeforeManufacture() {
        assertThatThrownBy(() -> new CarDetails("Clio", CarCondition.GOOD, Year.of(2020), Year.of(2019), "desc"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("yearRegistered");
    }

    @Test
    void descriptionIsOptional() {
        assertThat(new CarDetails("Clio", CarCondition.BAD, Year.of(2020), Year.of(2021), null).description()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void requiresCarName(String carName) {
        assertThatThrownBy(() -> new CarDetails(carName, CarCondition.GOOD, Year.of(2020), Year.of(2020), "desc"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("carName");
    }

    @Test
    void requiresConditionAndYears() {
        assertThatThrownBy(() -> new CarDetails("Clio", null, Year.of(2020), Year.of(2020), "desc"))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("condition");
        assertThatThrownBy(() -> new CarDetails("Clio", CarCondition.GOOD, null, Year.of(2020), "desc"))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("yearMade");
        assertThatThrownBy(() -> new CarDetails("Clio", CarCondition.GOOD, Year.of(2020), null, "desc"))
                .isInstanceOf(DomainValidationException.class).hasMessageContaining("yearRegistered");
    }
}
