package com.ridehailing.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(
        @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins,
        @Value("${cors.allow-credentials:true}") boolean allowCredentials,
        @Value("${cors.max-age:3600}") long maxAge
    ) {
        CorsConfiguration config = createCorsConfiguration(allowedOrigins, allowCredentials, maxAge);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    public CorsConfiguration createCorsConfiguration(String allowedOrigins, boolean allowCredentials, long maxAge) {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        // Enforce W3C security standard: never use wildcard '*' when credentials are enabled
        for (String origin : origins) {
            if ("*".equals(origin) && allowCredentials) {
                throw new IllegalStateException("CORS configuration error: wildcard origin '*' cannot be used with allowCredentials=true");
            }
            config.addAllowedOrigin(origin);
        }

        config.setAllowCredentials(allowCredentials);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of(
            "X-User-Id",
            "X-User-Role",
            "X-User-Email",
            "Retry-After"
        ));
        config.setMaxAge(maxAge);

        return config;
    }
}
