package com.auction.app.web.stream;

import com.auction.app.web.security.OpenApiSecurity;
import com.auction.application.query.GetAuctionQuery;
import com.auction.application.query.GetAuctionUseCase;
import com.auction.domain.auction.AuctionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auctions")
public class AuctionStreamController {

    private final GetAuctionUseCase getAuction;
    private final AuctionStreamBroadcaster broadcaster;

    // no 'produces': SseEmitter sets text/event-stream itself, and errors must still render as problem+json
    @GetMapping("/api/auctions/{auctionId}/stream")
    @Operation(summary = "Live events of an auction (Server-Sent Events)",
            description = "Events: `connected`, `bid` (a new highest bid), `closed` (then the stream ends). "
                    + "A comment line is sent every 20 s as heartbeat. Reconnect when the stream ends.")
    @SecurityRequirement(name = OpenApiSecurity.BEARER_JWT)
    @ApiResponse(responseCode = "200", description = "text/event-stream")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    public SseEmitter stream(@PathVariable UUID auctionId, @Parameter(hidden = true) HttpServletResponse response) {
        AuctionId id = new AuctionId(auctionId);
        getAuction.handle(new GetAuctionQuery(id));
        // nginx must not buffer the stream
        response.setHeader("X-Accel-Buffering", "no");
        return broadcaster.subscribe(id);
    }
}
