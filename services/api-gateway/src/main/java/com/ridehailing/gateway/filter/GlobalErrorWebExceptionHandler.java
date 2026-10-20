package com.ridehailing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2) // High precedence before Spring's DefaultErrorWebExceptionHandler (-1)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An internal gateway error occurred";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Target service unavailable or connection refused";
        } else if (ex.getClass().getName().contains("Redis")) {
            log.warn("Redis operational warning: Rate limiting backend unavailable: {}", ex.getMessage());
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Rate limiting service temporarily unavailable";
        }

        log.warn("Gateway error handled for [{} {}]: {} - {}",
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath(),
            status,
            message);

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
        } catch (Exception e) {
            String fallback = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.Instant.now(), status.value(), status.getReasonPhrase(), message, exchange.getRequest().getPath().value()
            );
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
