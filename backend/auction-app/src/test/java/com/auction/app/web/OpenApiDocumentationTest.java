package com.auction.app.web;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasKey;
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
                .andExpect(jsonPath("$.paths['/api/auctions'].post.parameters").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").value("JWT"));
    }

    @Test
    void documentsWhichOperationsNeedAToken() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/auctions'].post.security[0]").value(hasKey("bearer-jwt")))
                .andExpect(jsonPath("$.paths['/api/auctions/{auctionId}/bids'].post.security[0]").value(hasKey("bearer-jwt")))
                .andExpect(jsonPath("$.paths['/api/auctions/{auctionId}'].get.security[0]").value(hasKey("bearer-jwt")))
                .andExpect(jsonPath("$.paths['/api/auctions/running'].get.security").doesNotExist())
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auctions'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/auctions/running'].get.responses['401']").doesNotExist());
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
