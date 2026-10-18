package com.auction.app.web.auction;

import com.auction.application.command.PlaceBidCommand;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlaceBidRequest(
        @NotNull @Positive @Digits(integer = 17, fraction = 2) @Schema(example = "1250.00") BigDecimal amount) {

    PlaceBidCommand toCommand(AuctionId auctionId, UserId bidderId) {
        return new PlaceBidCommand(auctionId, bidderId, new Money(amount));
    }
}
