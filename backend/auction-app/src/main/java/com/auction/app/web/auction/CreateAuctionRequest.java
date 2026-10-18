package com.auction.app.web.auction;

import com.auction.application.command.CreateAuctionCommand;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;

/**
 * Format checks only; business rules (endDate after startDate, yearRegistered >= yearMade) stay in the domain.
 */
public record CreateAuctionRequest(
        @NotBlank @Size(max = 200) @Schema(example = "Peugeot 308") String carName,
        @NotNull CarCondition condition,
        @NotNull @Min(1886) @Max(9999) @Schema(example = "2018") Integer yearMade,
        @NotNull @Min(1886) @Max(9999) @Schema(example = "2019") Integer yearRegistered,
        @Size(max = 4000) @Schema(example = "One owner, full service history") String description,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) @Schema(example = "1000.00") BigDecimal startPrice,
        @NotNull @Schema(example = "2026-11-01T09:00:00Z") Instant startDate,
        @NotNull @Schema(example = "2026-11-08T18:00:00Z") Instant endDate) {

    CreateAuctionCommand toCommand(UserId sellerId) {
        return new CreateAuctionCommand(sellerId, carName, condition, Year.of(yearMade), Year.of(yearRegistered),
                description, new Money(startPrice), startDate, endDate);
    }
}
