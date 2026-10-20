package com.ridehailing.gateway.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);
    public static final String DEFAULT_DEV_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final String secret;
    private final ObjectMapper objectMapper;
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder urlDecoder = Base64.getUrlDecoder();

    @Autowired
    public JwtTokenValidator(
        @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
        ObjectMapper objectMapper,
        Environment environment
    ) {
        this.objectMapper = objectMapper;

        boolean isProduction = false;
        if (environment != null && environment.getActiveProfiles() != null) {
            isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        }

        if (isProduction) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("JWT secret must be explicitly provided in production (set JWT_SECRET)");
            }
            if (DEFAULT_DEV_SECRET.equals(secret)) {
                throw new IllegalStateException("Production environment must not use the default development JWT secret");
            }
            if (secret.length() < 32) {
                throw new IllegalStateException("Production JWT secret must be at least 32 characters long");
            }
        }

        this.secret = (secret != null && !secret.isBlank()) ? secret : DEFAULT_DEV_SECRET;
    }

    public JwtValidationResult validateAndExtract(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return JwtValidationResult.invalid("Missing Authorization header");
        }
        if (!authHeader.startsWith("Bearer ")) {
            return JwtValidationResult.invalid("Authorization header must use Bearer scheme");
        }

        String token = authHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return JwtValidationResult.invalid("Malformed JWT token structure");
        }

        String dataToSign = parts[0] + "." + parts[1];
        String expectedSignature;
        try {
            expectedSignature = sign(dataToSign);
        } catch (Exception e) {
            return JwtValidationResult.invalid("Failed to calculate HMAC signature");
        }

        // Constant-time signature verification
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            return JwtValidationResult.invalid("Invalid JWT token signature");
        }

        try {
            byte[] payloadBytes = urlDecoder.decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<HashMap<String, Object>>() {});

            Number expNumber = (Number) claims.get("exp");
            if (expNumber != null && Instant.now().getEpochSecond() > expNumber.longValue()) {
                return JwtValidationResult.invalid("JWT token has expired");
            }

            Object subObj = claims.get("sub");
            if (subObj == null) {
                return JwtValidationResult.invalid("JWT token missing subject claim");
            }

            Long userId;
            try {
                userId = Long.valueOf(subObj.toString());
            } catch (NumberFormatException e) {
                return JwtValidationResult.invalid("JWT token subject is not a valid user ID");
            }

            String email = (String) claims.get("email");
            String role = (String) claims.getOrDefault("role", "ROLE_PASSENGER");

            return JwtValidationResult.valid(new AuthenticatedUser(userId, email, role));
        } catch (Exception e) {
            return JwtValidationResult.invalid("Failed to parse JWT claims");
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return urlEncoder.encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }
}
