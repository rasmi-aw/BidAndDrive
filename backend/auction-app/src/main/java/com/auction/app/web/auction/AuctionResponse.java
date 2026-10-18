package com.auction.app.web.auction;

import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.auction.CarCondition;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Relation(itemRelation = "auction", collectionRelation = "auctions")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionResponse(
        UUID id,
        UUID sellerId,
        String carName,
        CarCondition condition,
        int yearMade,
        int yearRegistered,
        String description,
        BigDecimal startPrice,
        BigDecimal currentHighestBid,
        Instant startDate,
        Instant endDate,
        AuctionStatus status) {

    static AuctionResponse from(AuctionSummary summary) {
        return new AuctionResponse(
                summary.id().value(),
                summary.sellerId().value(),
                summary.carName(),
                summary.condition(),
                summary.yearMade().getValue(),
                summary.yearRegistered().getValue(),
                summary.description(),
                summary.startPrice().amount(),
                summary.currentHighestBid() == null ? null : summary.currentHighestBid().amount(),
                summary.startDate(),
                summary.endDate(),
                summary.status());
    }
}
