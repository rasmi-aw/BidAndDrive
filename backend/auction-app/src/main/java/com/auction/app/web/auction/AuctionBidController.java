package com.auction.app.web.auction;

import com.auction.app.web.PagedModels;
import com.auction.app.web.security.CurrentUser;
import com.auction.app.web.security.OpenApiSecurity;
import com.auction.application.command.PlaceBidUseCase;
import com.auction.application.query.ListAuctionBidsQuery;
import com.auction.application.query.ListAuctionBidsUseCase;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidId;
import com.auction.domain.shared.Page;
import com.auction.domain.user.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiSecurity.BEARER_JWT)
@Tag(name = "Bids")
public class AuctionBidController {

    static final int DEFAULT_SIZE = 20;

    private final PlaceBidUseCase placeBid;
    private final ListAuctionBidsUseCase listAuctionBids;

    @PostMapping
    @Operation(summary = "Place a bid", description = "Only a BUYER can bid, never on their own auction. "
            + "The amount must be greater than both the start price and the current highest bid.")
    @ApiResponse(responseCode = "201", description = "Bid accepted")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Caller is not a BUYER, is the seller of the auction, "
            + "or the token lacks profile claims")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    @ApiResponse(responseCode = "409", description = "Bid too low, auction not open for bidding, "
            + "or another bid was accepted at the same time (reload and retry)")
    public ResponseEntity<EntityModel<BidResponse>> placeBid(
            @PathVariable UUID auctionId,
            @CurrentUser UserId userId,
            @Valid @RequestBody PlaceBidRequest request) {
        BidId bidId = placeBid.handle(request.toCommand(new AuctionId(auctionId), userId));
        BidResponse body = new BidResponse(bidId.value(), auctionId, userId.value(), request.amount(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(withLinks(body));
    }

    @GetMapping
    @Operation(summary = "List the bids of an auction", description = "Newest first.")
    @ApiResponse(responseCode = "200", description = "One page of bids")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    public PagedModel<EntityModel<BidResponse>> listBids(
            @PathVariable UUID auctionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(1) @Max(100) int size) {
        Page<Bid> result = listAuctionBids.handle(ListAuctionBidsQuery.of(new AuctionId(auctionId), page, size));
        return PagedModels.of(result, result.content().stream().map(BidResponse::from).map(this::withLinks).toList(),
                p -> linkTo(methodOn(AuctionBidController.class).listBids(auctionId, p, size)).withSelfRel());
    }

    private EntityModel<BidResponse> withLinks(BidResponse bid) {
        return EntityModel.of(bid,
                linkTo(methodOn(AuctionController.class).getAuction(bid.auctionId())).withRel("auction"),
                linkTo(AuctionBidController.class, bid.auctionId()).withRel(AuctionModelAssembler.BIDS));
    }
}
