package com.auction.app.web;

import com.auction.application.command.PlaceBidUseCase;
import com.auction.domain.auction.AuctionConcurrentlyModifiedException;
import com.auction.domain.auction.AuctionId;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A real optimistic-lock race is covered in auction-infrastructure (ConcurrentBiddingTest); here we only check the
 * HTTP mapping, so the use case is replaced by a mock that loses the race.
 */
class ConcurrentBidConflictApiTest extends ApiIntegrationTest {

    @MockitoBean
    private PlaceBidUseCase placeBid;

    @Test
    void optimisticLockConflictIsA409() throws Exception {
        UUID auctionId = UUID.randomUUID();
        when(placeBid.handle(any())).thenThrow(new AuctionConcurrentlyModifiedException(new AuctionId(auctionId)));

        postBid(auctionId, buyer(), "2000")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:concurrent-modification"))
                .andExpect(jsonPath("$.detail", containsString("retry")));
    }

    @Test
    void untranslatedHibernateConflictIsAlsoA409WithoutLeakingInternals() throws Exception {
        when(placeBid.handle(any())).thenThrow(new ObjectOptimisticLockingFailureException("AuctionEntity", "id"));

        postBid(UUID.randomUUID(), buyer(), "2000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:bidanddrive:problem:concurrent-modification"))
                .andExpect(jsonPath("$.detail", not(containsString("Entity"))));
    }
}
