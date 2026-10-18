package com.auction.app.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

@Configuration
class OpenApiConfig {

    private static final String PROBLEM_DETAIL = "ProblemDetail";
    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Bean
    OpenAPI auctionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BidAndDrive Auction API")
                .version("v1")
                .description("Car auctions: sellers open auctions, buyers bid. Responses use HAL links; "
                        + "errors use RFC 9457 problem details. The caller is identified by the X-User-Id header "
                        + "until authentication is added."));
    }

    /**
     * Documents every 4xx/5xx response as application/problem+json with the {@link ProblemDetail} schema.
     */
    @Bean
    OpenApiCustomizer problemDetailResponses() {
        return openApi -> {
            ModelConverters.getInstance().read(ProblemDetail.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            Content problemContent = new Content().addMediaType(PROBLEM_JSON,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL)));
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                    operation.getResponses().forEach((code, response) -> {
                        if (code.startsWith("4") || code.startsWith("5")) {
                            response.setContent(problemContent);
                        }
                    })));
        };
    }
}
