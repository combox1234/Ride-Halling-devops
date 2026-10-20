package com.ridehailing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.ApiErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitResponseDecoratorFilter implements WebFilter {

    private final ObjectMapper objectMapper;

    @Autowired
    public RateLimitResponseDecoratorFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> setComplete() {
                if (HttpStatus.TOO_MANY_REQUESTS.equals(getStatusCode()) && !isCommitted()) {
                    getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    ApiErrorResponse errorResponse = ApiErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        "Rate limit exceeded. Please try again later.",
                        exchange.getRequest().getPath().value()
                    );

                    byte[] bytes;
                    try {
                        bytes = objectMapper.writeValueAsBytes(errorResponse);
                    } catch (Exception e) {
                        String fallback = String.format(
                            "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"path\":\"%s\"}",
                            java.time.Instant.now(), exchange.getRequest().getPath().value()
                        );
                        bytes = fallback.getBytes(StandardCharsets.UTF_8);
                    }

                    DataBuffer buffer = bufferFactory().wrap(bytes);
                    return writeWith(Mono.just(buffer));
                }
                return super.setComplete();
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }
}
