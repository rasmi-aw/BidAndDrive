package com.auction.app.web.auction;

import com.auction.domain.auction.AuctionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Links: self, bids, and place-bid only while the auction accepts bids.
 */
@Component
@RequiredArgsConstructor
class AuctionModelAssembler implements RepresentationModelAssembler<AuctionSummary, EntityModel<AuctionResponse>> {

    static final String PLACE_BID = "place-bid";
    static final String BIDS = "bids";

    private final Clock clock;

    @Override
    public EntityModel<AuctionResponse> toModel(AuctionSummary summary) {
        UUID id = summary.id().value();
        EntityModel<AuctionResponse> model = EntityModel.of(AuctionResponse.from(summary),
                linkTo(methodOn(AuctionController.class).getAuction(id)).withSelfRel(),
                linkTo(AuctionBidController.class, id).withRel(BIDS));
        if (summary.isOpenForBidding(clock.instant())) {
            model.add(linkTo(methodOn(AuctionBidController.class).placeBid(id, null, null)).withRel(PLACE_BID));
        }
        return model;
    }
}
