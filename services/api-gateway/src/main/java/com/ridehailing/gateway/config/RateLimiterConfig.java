package com.ridehailing.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    public static final String ATTR_USER_ID = "authenticatedUserId";
    public static final String DEFAULT_ANONYMOUS_KEY = "ip:anonymous-unknown";

    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            // 1. Check for authenticated user ID extracted by JwtAuthenticationFilter
            Object authUserId = exchange.getAttribute(ATTR_USER_ID);
            if (authUserId != null) {
                return Mono.just("user:" + authUserId);
            }

            String xUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (xUserId != null && !xUserId.isBlank()) {
                return Mono.just("user:" + xUserId.trim());
            }

            // 2. Fallback to client IP for unauthenticated / public endpoints
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                String hostAddress = remoteAddress.getAddress().getHostAddress();
                if (hostAddress != null && !hostAddress.isBlank()) {
                    return Mono.just("ip:" + hostAddress.trim());
                }
            }

            // 3. Deterministic safe fallback when remote address is unavailable
            return Mono.just(DEFAULT_ANONYMOUS_KEY);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter(
        @Value("${gateway.rate-limiter.replenish-rate:10}") int defaultReplenishRate,
        @Value("${gateway.rate-limiter.burst-capacity:20}") int defaultBurstCapacity,
        @Value("${gateway.rate-limiter.requested-tokens:1}") int defaultRequestedTokens
    ) {
        return new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity, defaultRequestedTokens);
    }
}
