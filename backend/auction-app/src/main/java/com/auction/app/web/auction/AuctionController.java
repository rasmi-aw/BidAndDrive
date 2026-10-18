package com.auction.app.web.auction;

import com.auction.app.web.ApiHeaders;
import com.auction.app.web.PagedModels;
import com.auction.application.command.CreateAuctionUseCase;
import com.auction.application.query.GetAuctionQuery;
import com.auction.application.query.GetAuctionUseCase;
import com.auction.application.query.ListRunningAuctionsQuery;
import com.auction.application.query.ListRunningAuctionsUseCase;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.shared.Page;
import com.auction.domain.user.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Tag(name = "Auctions")
public class AuctionController {

    static final int DEFAULT_SIZE = 20;

    private final CreateAuctionUseCase createAuction;
    private final GetAuctionUseCase getAuction;
    private final ListRunningAuctionsUseCase listRunningAuctions;
    private final AuctionModelAssembler assembler;

    @PostMapping
    @Operation(summary = "Create an auction", description = "Only a SELLER can create an auction.")
    @ApiResponse(responseCode = "201", description = "Auction created and running")
    @ApiResponse(responseCode = "400", description = "Invalid request or business rule violated")
    @ApiResponse(responseCode = "403", description = "Caller is not a SELLER")
    @ApiResponse(responseCode = "404", description = "Caller not found")
    public ResponseEntity<EntityModel<AuctionResponse>> createAuction(
            @Parameter(description = "Id of the calling user (stand-in for authentication)")
            @RequestHeader(ApiHeaders.USER_ID) UUID userId,
            @Valid @RequestBody CreateAuctionRequest request) {
        AuctionId id = createAuction.handle(request.toCommand(new UserId(userId)));
        EntityModel<AuctionResponse> model = assembler.toModel(getAuction.handle(new GetAuctionQuery(id)));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    @GetMapping("/running")
    @Operation(summary = "List running auctions", description = "Public. Auctions open for bidding now, ending soonest first.")
    @ApiResponse(responseCode = "200", description = "One page of running auctions")
    @ApiResponse(responseCode = "400", description = "Invalid paging parameters")
    public PagedModel<EntityModel<AuctionResponse>> listRunningAuctions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(1) @Max(100) int size) {
        Page<AuctionSummary> result = listRunningAuctions.handle(ListRunningAuctionsQuery.of(page, size));
        return PagedModels.of(result, result.content().stream().map(assembler::toModel).toList(),
                p -> linkTo(methodOn(AuctionController.class).listRunningAuctions(p, size)).withSelfRel());
    }

    @GetMapping("/{auctionId}")
    @Operation(summary = "Get an auction")
    @ApiResponse(responseCode = "200", description = "The auction")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    public EntityModel<AuctionResponse> getAuction(@PathVariable UUID auctionId) {
        return assembler.toModel(getAuction.handle(new GetAuctionQuery(new AuctionId(auctionId))));
    }
}
