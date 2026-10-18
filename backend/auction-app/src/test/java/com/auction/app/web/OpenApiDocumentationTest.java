package com.auction.app.web;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocumentationTest extends ApiIntegrationTest {

    @Test
    void documentsTheApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("BidAndDrive Auction API"))
                .andExpect(jsonPath("$.paths['/api/auctions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auctions/running'].get").exists())
                .andExpect(jsonPath("$.paths['/api/auctions/{auctionId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/auctions/{auctionId}/bids'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auctions/{auctionId}/bids'].get").exists())
                .andExpect(jsonPath("$.paths['/api/auctions'].post.parameters[*].name", hasItem("X-User-Id")));
    }

    @Test
    void errorResponsesAreDocumentedAsProblemDetails() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/auctions/{auctionId}/bids'].post.responses['409']"
                        + ".content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.paths['/api/auctions'].post.responses['403']"
                        + ".content['application/problem+json']").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
