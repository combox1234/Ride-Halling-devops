package com.ridehailing.gateway;

import com.ridehailing.gateway.security.JwtTokenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRouteIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    private final String devSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Test
    @DisplayName("Health endpoint: Should be public and return 200 OK")
    void healthEndpointShouldBePublic() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Protected route without token: Should return 401 Unauthorized with standard ApiErrorResponse")
    void protectedRouteWithoutTokenShouldReturn401() {
        webTestClient.post()
            .uri("/api/v1/rides/request")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"passengerId\": 1}")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.message").isEqualTo("Missing Authorization header")
            .jsonPath("$.path").isEqualTo("/api/v1/rides/request");
    }

    @Test
    @DisplayName("Protected route with expired token: Should return 401 Unauthorized")
    void protectedRouteWithExpiredTokenShouldReturn401() {
        String expiredHeader = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String expiredPayload = encoder.encodeToString(String.format(
            "{\"sub\":\"1\",\"email\":\"u@r.com\",\"role\":\"ROLE_PASSENGER\",\"iat\":%d,\"exp\":%d}",
            Instant.now().getEpochSecond() - 7200, Instant.now().getEpochSecond() - 3600
        ).getBytes(StandardCharsets.UTF_8));
        String data = expiredHeader + "." + expiredPayload;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(devSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig = encoder.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            String expiredToken = data + "." + sig;

            webTestClient.get()
                .uri("/api/v1/passengers/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("JWT token has expired");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
