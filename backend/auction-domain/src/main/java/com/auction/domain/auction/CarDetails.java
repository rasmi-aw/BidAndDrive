package com.auction.domain.auction;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Guard;

import java.time.Year;

/**
 * The car being auctioned. {@code description} is optional.
 */
public record CarDetails(String carName, CarCondition condition, Year yearMade, Year yearRegistered, String description) {

    public CarDetails {
        carName = Guard.notBlank(carName, "carName");
        Guard.notNull(condition, "condition");
        Guard.notNull(yearMade, "yearMade");
        Guard.notNull(yearRegistered, "yearRegistered");
        if (yearRegistered.isBefore(yearMade)) {
            throw new DomainValidationException(
                    "yearRegistered (" + yearRegistered + ") must not be before yearMade (" + yearMade + ")");
        }
    }
}
