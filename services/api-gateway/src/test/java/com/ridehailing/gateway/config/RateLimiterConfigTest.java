package com.ridehailing.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

class RateLimiterConfigTest {

    private KeyResolver keyResolver;
    private RateLimiterConfig rateLimiterConfig;

    @BeforeEach
    void setUp() {
        rateLimiterConfig = new RateLimiterConfig();
        keyResolver = rateLimiterConfig.apiKeyResolver();
    }

    @Test
    @DisplayName("Should resolve rate limit key from authenticated user ID attribute")
    void shouldResolveFromUserAttribute() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/rides/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(RateLimiterConfig.ATTR_USER_ID, 555L);

        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext("user:555")
            .verifyComplete();
    }

    @Test
    @DisplayName("Should resolve rate limit key from X-User-Id header if present")
    void shouldResolveFromUserIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/rides/1")
            .header("X-User-Id", "777")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext("user:777")
            .verifyComplete();
    }

    @Test
    @DisplayName("Should resolve rate limit key from remote client IP for unauthenticated requests")
    void shouldResolveFromRemoteIp() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
            .remoteAddress(new InetSocketAddress("203.0.113.195", 54321))
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext("ip:203.0.113.195")
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fallback to anonymous-unknown when remote address is unavailable")
    void shouldFallbackWhenRemoteAddressNull() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext(RateLimiterConfig.DEFAULT_ANONYMOUS_KEY)
            .verifyComplete();
    }
}
