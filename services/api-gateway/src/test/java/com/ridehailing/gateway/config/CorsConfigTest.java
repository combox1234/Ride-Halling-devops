package com.ridehailing.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    @DisplayName("Should permit allowed origin in CORS configuration")
    void shouldPermitAllowedOrigin() {
        CorsConfiguration config = corsConfig.createCorsConfiguration("http://localhost:3000,http://localhost:5173", true, 3600);

        assertThat(config.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(config.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Test
    @DisplayName("Should reject disallowed origin in CORS configuration")
    void shouldRejectDisallowedOrigin() {
        CorsConfiguration config = corsConfig.createCorsConfiguration("http://localhost:3000,http://localhost:5173", true, 3600);

        assertThat(config.checkOrigin("http://malicious-attacker.com")).isNull();
        assertThat(config.checkOrigin("http://evil.com")).isNull();
    }

    @Test
    @DisplayName("Should throw IllegalStateException when wildcard origin is combined with allowCredentials")
    void shouldRejectWildcardWithCredentials() {
        assertThatThrownBy(() -> corsConfig.createCorsConfiguration("*", true, 3600))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("wildcard origin '*' cannot be used with allowCredentials=true");
    }

    @Test
    @DisplayName("UrlBasedCorsConfigurationSource should match paths correctly")
    void shouldMatchUrlBasedConfigurationSource() {
        CorsConfiguration config = corsConfig.createCorsConfiguration("http://localhost:3000", true, 3600);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        MockServerHttpRequest request = MockServerHttpRequest.options("/api/v1/auth/login")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        CorsConfiguration resolved = source.getCorsConfiguration(exchange);
        assertThat(resolved).isNotNull();
        assertThat(resolved.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
    }
}
