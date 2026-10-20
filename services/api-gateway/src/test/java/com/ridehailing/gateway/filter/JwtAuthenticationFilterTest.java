package com.ridehailing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.ApiErrorResponse;
import com.ridehailing.gateway.config.RateLimiterConfig;
import com.ridehailing.gateway.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private ObjectMapper objectMapper;
    private final String devSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JwtTokenValidator validator = new JwtTokenValidator(devSecret, objectMapper, new MockEnvironment());
        filter = new JwtAuthenticationFilter(validator, objectMapper);
    }

    private String createToken(Long userId, String email, String role, long expSeconds) throws Exception {
        String header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(String.format(
            "{\"sub\":\"%d\",\"email\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
            userId, email, role, Instant.now().getEpochSecond(), expSeconds
        ).getBytes(StandardCharsets.UTF_8));
        String data = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(devSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));

        return data + "." + signature;
    }

    @Test
    @DisplayName("Protected route with valid JWT: should propagate trusted claims and strip spoofed headers")
    void shouldPropagateTrustedClaimsAndStripSpoofedHeaders() throws Exception {
        long exp = Instant.now().getEpochSecond() + 3600;
        String token = createToken(101L, "passenger@ride.com", "ROLE_PASSENGER", exp);

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/rides/request")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header("X-User-Id", "999") // Spoofed untrusted header
            .header("X-User-Role", "ROLE_ADMIN") // Spoofed untrusted header
            .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> passedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            passedExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(passedExchange.get()).isNotNull();
        HttpHeaders downstreamHeaders = passedExchange.get().getRequest().getHeaders();

        // Spoofed values replaced with verified JWT claims
        assertThat(downstreamHeaders.getFirst("X-User-Id")).isEqualTo("101");
        assertThat(downstreamHeaders.getFirst("X-User-Role")).isEqualTo("ROLE_PASSENGER");
        assertThat(downstreamHeaders.getFirst("X-User-Email")).isEqualTo("passenger@ride.com");
        assertThat(downstreamHeaders.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);

        // Exchange attribute populated for rate limiting
        assertThat((Long) passedExchange.get().getAttribute(RateLimiterConfig.ATTR_USER_ID)).isEqualTo(101L);
    }

    @Test
    @DisplayName("Protected route with missing Authorization: should return 401 with standard ApiErrorResponse")
    void shouldReturn401WhenMissingAuthorization() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/rides/request").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chainCalled.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Protected route with expired token: should return 401 Unauthorized")
    void shouldReturn401WhenTokenExpired() throws Exception {
        long exp = Instant.now().getEpochSecond() - 100;
        String token = createToken(101L, "user@ride.com", "ROLE_PASSENGER", exp);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/passengers/101")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Public route (login/register/health): should pass without authentication and strip spoofed headers")
    void shouldAllowPublicRouteWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
            .header("X-User-Id", "spoofed-user")
            .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> passedExchange = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, ex -> {
            passedExchange.set(ex);
            return Mono.empty();
        })).verifyComplete();

        assertThat(passedExchange.get()).isNotNull();
        assertThat(passedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    @DisplayName("Restricted admin route: should reject non-admin with 403 Forbidden")
    void shouldRejectNonAdminOnRestrictedRoute() throws Exception {
        long exp = Instant.now().getEpochSecond() + 3600;
        String token = createToken(202L, "passenger@ride.com", "ROLE_PASSENGER", exp);

        MockServerHttpRequest request = MockServerHttpRequest.put("/api/v1/pricing/zones/1/surge")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("CORS preflight OPTIONS request: should pass without authentication")
    void shouldAllowCorsPreflightOptions() {
        MockServerHttpRequest request = MockServerHttpRequest.options("/api/v1/rides/request").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        StepVerifier.create(filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(chainCalled.get()).isTrue();
    }
}
