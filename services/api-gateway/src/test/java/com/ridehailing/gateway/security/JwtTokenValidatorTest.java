package com.ridehailing.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private ObjectMapper objectMapper;
    private final String devSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MockEnvironment devEnv = new MockEnvironment();
        validator = new JwtTokenValidator(devSecret, objectMapper, devEnv);
    }

    private String createToken(Long userId, String email, String role, long expSeconds, String signingSecret) throws Exception {
        String header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(String.format(
            "{\"sub\":\"%d\",\"email\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
            userId, email, role, Instant.now().getEpochSecond(), expSeconds
        ).getBytes(StandardCharsets.UTF_8));
        String data = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));

        return data + "." + signature;
    }

    @Test
    @DisplayName("Should successfully validate token and extract claims")
    void shouldValidateAndExtractClaims() throws Exception {
        long exp = Instant.now().getEpochSecond() + 3600;
        String token = createToken(42L, "driver@ride.com", "ROLE_DRIVER", exp, devSecret);

        JwtValidationResult result = validator.validateAndExtract("Bearer " + token);

        assertThat(result.isValid()).isTrue();
        assertThat(result.user()).isNotNull();
        assertThat(result.user().userId()).isEqualTo(42L);
        assertThat(result.user().email()).isEqualTo("driver@ride.com");
        assertThat(result.user().role()).isEqualTo("ROLE_DRIVER");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() throws Exception {
        long exp = Instant.now().getEpochSecond() - 60; // 1 min ago
        String token = createToken(1L, "user@ride.com", "ROLE_PASSENGER", exp, devSecret);

        JwtValidationResult result = validator.validateAndExtract("Bearer " + token);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("expired");
    }

    @Test
    @DisplayName("Should reject tampered token signature")
    void shouldRejectTamperedTokenSignature() throws Exception {
        long exp = Instant.now().getEpochSecond() + 3600;
        String token = createToken(1L, "user@ride.com", "ROLE_PASSENGER", exp, "different-secret-key-32-chars-long!");

        JwtValidationResult result = validator.validateAndExtract("Bearer " + token);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("Invalid JWT token signature");
    }

    @Test
    @DisplayName("Should reject missing or non-Bearer authorization header")
    void shouldRejectMissingOrNonBearerAuthHeader() {
        assertThat(validator.validateAndExtract(null).isValid()).isFalse();
        assertThat(validator.validateAndExtract("").isValid()).isFalse();
        assertThat(validator.validateAndExtract("Basic abc123xyz").isValid()).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed JWT token structure")
    void shouldRejectMalformedToken() {
        JwtValidationResult result = validator.validateAndExtract("Bearer header.payload");
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("Malformed JWT");
    }

    @Test
    @DisplayName("Production profile: Should fail fast when secret is empty or default")
    void shouldFailFastInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        assertThatThrownBy(() -> new JwtTokenValidator("", objectMapper, prodEnv))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("explicitly provided in production");

        assertThatThrownBy(() -> new JwtTokenValidator(JwtTokenValidator.DEFAULT_DEV_SECRET, objectMapper, prodEnv))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must not use the default development JWT secret");

        assertThatThrownBy(() -> new JwtTokenValidator("short-key", objectMapper, prodEnv))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 characters long");
    }
}
