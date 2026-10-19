package com.ridehailing.passenger.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private final String devSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockEnvironment devEnvironment = new MockEnvironment();

    @BeforeEach
    void setUp() {
        devEnvironment.setActiveProfiles("dev");
        validator = new JwtTokenValidator(devSecret, objectMapper, devEnvironment);
    }

    @Test
    @DisplayName("Should successfully validate token and extract authenticated user")
    void shouldValidateTokenAndExtractUser() {
        String token = "Bearer " + createToken(201L, "driver@example.com", "ROLE_DRIVER", 3600, devSecret);
        AuthenticatedUser user = validator.validateAndExtract(token);

        assertThat(user.userId()).isEqualTo(201L);
        assertThat(user.email()).isEqualTo("driver@example.com");
        assertThat(user.role()).isEqualTo("ROLE_DRIVER");
    }

    @Test
    @DisplayName("Should reject null or blank Authorization header with 401")
    void shouldRejectNullOrBlankHeader() {
        assertThatThrownBy(() -> validator.validateAndExtract(null))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Missing or empty Authorization header");

        assertThatThrownBy(() -> validator.validateAndExtract("   "))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should reject non-Bearer authorization scheme with 401")
    void shouldRejectNonBearerScheme() {
        assertThatThrownBy(() -> validator.validateAndExtract("Basic dXNlcjpwYXNz"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("must use Bearer scheme");
    }

    @Test
    @DisplayName("Should reject token with invalid signature with 401")
    void shouldRejectInvalidSignature() {
        String token = "Bearer " + createToken(201L, "user@example.com", "ROLE_PASSENGER", 3600, "WrongSecretKeyThatDoesNotMatchTheConfiguredOne123!");
        assertThatThrownBy(() -> validator.validateAndExtract(token))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid JWT token signature");
    }

    @Test
    @DisplayName("Should reject expired token with 401")
    void shouldRejectExpiredToken() {
        String token = "Bearer " + createToken(201L, "user@example.com", "ROLE_PASSENGER", -60, devSecret);
        assertThatThrownBy(() -> validator.validateAndExtract(token))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Production profile: should reject missing secret in passenger-service")
    void shouldRejectMissingSecretInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtTokenValidator("", objectMapper, prodEnv))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT secret must be explicitly provided in production");
    }

    @Test
    @DisplayName("Production profile: should reject default dev secret in passenger-service")
    void shouldRejectDevSecretInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        assertThatThrownBy(() -> new JwtTokenValidator(JwtTokenValidator.DEFAULT_DEV_SECRET, objectMapper, prodEnv))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must not use the default development JWT secret");
    }

    private String createToken(Long userId, String email, String role, long validSeconds, String secretKey) {
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            long now = Instant.now().getEpochSecond();
            long exp = now + validSeconds;

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = String.format("{\"sub\":\"%d\",\"email\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId, email, role, now, exp);

            String encodedHeader = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String dataToSign = encodedHeader + "." + encodedPayload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = encoder.encodeToString(mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8)));

            return dataToSign + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
