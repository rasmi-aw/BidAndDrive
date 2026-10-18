package com.auction.app.config;

import com.auction.app.web.security.CurrentUser;
import com.auction.app.web.security.OpenApiSecurity;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

@Configuration
class OpenApiConfig {

    private static final String PROBLEM_DETAIL = "ProblemDetail";
    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    static {
        // the caller comes from the token, not from the request
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    OpenAPI auctionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BidAndDrive Auction API")
                        .version("v1")
                        .description("Car auctions: sellers open auctions, buyers bid. Responses use HAL links; "
                                + "errors use RFC 9457 problem details. Authenticate with a Keycloak access token "
                                + "(realm role SELLER or BUYER); only the running auctions list is public."))
                .components(new Components().addSecuritySchemes(OpenApiSecurity.BEARER_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
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
