package com.ridehailing.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.auth.model.UserAccount;
import com.ridehailing.auth.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockEnvironment devEnvironment = new MockEnvironment();

    @BeforeEach
    void setUp() {
        devEnvironment.setActiveProfiles("dev");
        jwtTokenService = new JwtTokenService(secret, 3600000, objectMapper, devEnvironment);
    }

    @Test
    @DisplayName("Should generate valid JWT token with user claims")
    void shouldGenerateValidToken() {
        UserAccount user = new UserAccount(101L, "Alice Rider", "alice@example.com", "hashed", UserRole.ROLE_PASSENGER, Instant.now());
        String token = jwtTokenService.generateToken(user);

        assertThat(token).isNotBlank();
        Optional<Map<String, Object>> claimsOpt = jwtTokenService.validateAndExtractClaims(token);

        assertThat(claimsOpt).isPresent();
        Map<String, Object> claims = claimsOpt.get();
        assertThat(claims.get("sub")).isEqualTo("101");
        assertThat(claims.get("email")).isEqualTo("alice@example.com");
        assertThat(claims.get("role")).isEqualTo("ROLE_PASSENGER");
    }

    @Test
    @DisplayName("Should validate token with Bearer prefix")
    void shouldValidateTokenWithBearerPrefix() {
        UserAccount user = new UserAccount(102L, "Bob Driver", "bob@example.com", "hashed", UserRole.ROLE_DRIVER, Instant.now());
        String token = "Bearer " + jwtTokenService.generateToken(user);

        Optional<Map<String, Object>> claimsOpt = jwtTokenService.validateAndExtractClaims(token);
        assertThat(claimsOpt).isPresent();
        assertThat(claimsOpt.get().get("email")).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("Should reject tampered token")
    void shouldRejectTamperedToken() {
        UserAccount user = new UserAccount(103L, "Carol", "carol@example.com", "hashed", UserRole.ROLE_PASSENGER, Instant.now());
        String token = jwtTokenService.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "ABCDE";

        Optional<Map<String, Object>> claimsOpt = jwtTokenService.validateAndExtractClaims(tampered);
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        JwtTokenService expiredService = new JwtTokenService(secret, -1000, objectMapper, devEnvironment);
        UserAccount user = new UserAccount(104L, "Dave", "dave@example.com", "hashed", UserRole.ROLE_PASSENGER, Instant.now());
        String token = expiredService.generateToken(user);

        Optional<Map<String, Object>> claimsOpt = jwtTokenService.validateAndExtractClaims(token);
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    @DisplayName("Production profile: should reject blank or null JWT secret")
    void shouldRejectBlankSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtTokenService("", 3600000, objectMapper, prodEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT secret must be explicitly provided in production");
    }

    @Test
    @DisplayName("Production profile: should reject fallback to known development secret")
    void shouldRejectDevSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("production");

        assertThatThrownBy(() -> new JwtTokenService(JwtTokenService.DEFAULT_DEV_SECRET, 3600000, objectMapper, prodEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Production environment must not use the default development JWT secret");
    }

    @Test
    @DisplayName("Production profile: should reject secret shorter than 32 characters")
    void shouldRejectShortSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtTokenService("too-short-secret-key", 3600000, objectMapper, prodEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 characters");
    }

    @Test
    @DisplayName("Production profile: should accept valid 32+ character production secret")
    void shouldAcceptValidSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");
        String strongProdSecret = "StrongProdSecretWithAtLeast32CharactersForProductionUse123!";

        JwtTokenService prodJwtService = new JwtTokenService(strongProdSecret, 3600000, objectMapper, prodEnvironment);
        UserAccount user = new UserAccount(105L, "Prod User", "prod@example.com", "hashed", UserRole.ROLE_PASSENGER, Instant.now());
        String token = prodJwtService.generateToken(user);

        assertThat(prodJwtService.validateAndExtractClaims(token)).isPresent();
    }
}
