package com.ridehailing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitResponseDecoratorFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RateLimitResponseDecoratorFilter filter = new RateLimitResponseDecoratorFilter(objectMapper);

    @Test
    @DisplayName("Should decorate 429 response with structured JSON ApiErrorResponse")
    void shouldDecorate429Response() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/rides/request").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return ex.getResponse().setComplete();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
