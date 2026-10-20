package com.ridehailing.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.ApiErrorResponse;
import com.ridehailing.gateway.config.RateLimiterConfig;
import com.ridehailing.gateway.security.AuthenticatedUser;
import com.ridehailing.gateway.security.JwtTokenValidator;
import com.ridehailing.gateway.security.JwtValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Filter executes before routing and rate limiting
    public static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    private final JwtTokenValidator jwtTokenValidator;
    private final ObjectMapper objectMapper;

    // Explicit public routes that do not require JWT authentication
    private final List<PublicRouteMatcher> publicRoutes = List.of(
        new PublicRouteMatcher(HttpMethod.POST, "/api/v1/auth/register"),
        new PublicRouteMatcher(HttpMethod.POST, "/api/v1/auth/login"),
        new PublicRouteMatcher(HttpMethod.GET, "/actuator/health"),
        new PublicRouteMatcher(HttpMethod.GET, "/actuator/info"),
        new PublicRouteMatcher(HttpMethod.POST, "/api/v1/pricing/estimate")
    );

    @Autowired
    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator, ObjectMapper objectMapper) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // 1. Strip all incoming client-supplied identity headers to prevent spoofing
        ServerHttpRequest.Builder requestBuilder = request.mutate()
            .headers(httpHeaders -> {
                httpHeaders.remove(HEADER_USER_ID);
                httpHeaders.remove(HEADER_USER_ROLE);
                httpHeaders.remove(HEADER_USER_EMAIL);
            });

        // 2. Allow preflight CORS OPTIONS requests to proceed without authentication
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // 3. Check public endpoint allowlist
        boolean isPublic = isPublicRoute(method, path);

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // If public route and no Authorization header, proceed with stripped headers
        if (isPublic && (authHeader == null || authHeader.isBlank())) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // 4. If public route with an Authorization header, attempt optional validation
        if (isPublic) {
            JwtValidationResult result = jwtTokenValidator.validateAndExtract(authHeader);
            if (result.isValid() && result.user() != null) {
                injectUserClaims(requestBuilder, exchange, result.user());
            }
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // 5. Protected route: Require Authorization Bearer header
        if (authHeader == null || authHeader.isBlank()) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }
        if (!authHeader.startsWith("Bearer ")) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Authorization header must use Bearer scheme");
        }

        // 6. Validate JWT token
        JwtValidationResult result = jwtTokenValidator.validateAndExtract(authHeader);
        if (!result.isValid() || result.user() == null) {
            String errorMsg = result.errorMessage() != null ? result.errorMessage() : "Invalid or expired JWT token";
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, errorMsg);
        }

        AuthenticatedUser user = result.user();

        // 7. Role authorization check for administrative/restricted paths
        if (path.startsWith("/api/v1/reporting/admin") || path.endsWith("/surge")) {
            if (!"ROLE_ADMIN".equals(user.role())) {
                return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "Access denied: ADMIN role required");
            }
        }

        // 8. Inject trusted claims into request headers and exchange attributes
        injectUserClaims(requestBuilder, exchange, user);

        // 9. Proceed down filter chain with mutated request
        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    private void injectUserClaims(ServerHttpRequest.Builder requestBuilder, ServerWebExchange exchange, AuthenticatedUser user) {
        requestBuilder.header(HEADER_USER_ID, String.valueOf(user.userId()));
        requestBuilder.header(HEADER_USER_ROLE, user.role());
        if (user.email() != null && !user.email().isBlank()) {
            requestBuilder.header(HEADER_USER_EMAIL, user.email());
        }
        exchange.getAttributes().put(RateLimiterConfig.ATTR_USER_ID, user.userId());
    }

    private boolean isPublicRoute(HttpMethod method, String path) {
        for (PublicRouteMatcher matcher : publicRoutes) {
            if (matcher.matches(method, path)) {
                return true;
            }
        }
        return false;
    }

    public Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            String fallback = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.Instant.now(), status.value(), status.getReasonPhrase(), message, exchange.getRequest().getPath().value()
            );
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private record PublicRouteMatcher(HttpMethod method, String path) {
        public boolean matches(HttpMethod reqMethod, String reqPath) {
            if (this.method != null && reqMethod != null && !this.method.equals(reqMethod)) {
                return false;
            }
            return reqPath != null && reqPath.equals(this.path);
        }
    }
}
