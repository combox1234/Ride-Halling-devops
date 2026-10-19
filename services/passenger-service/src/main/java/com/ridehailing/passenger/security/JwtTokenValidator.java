package com.ridehailing.passenger.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.exception.UnauthorizedException;
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

    public AuthenticatedUser validateAndExtract(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new UnauthorizedException("Missing or empty Authorization header");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header must use Bearer scheme");
        }

        String token = authHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("Malformed JWT token structure");
        }

        String dataToSign = parts[0] + "." + parts[1];
        String expectedSignature = sign(dataToSign);

        // Constant-time signature verification
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new UnauthorizedException("Invalid JWT token signature");
        }

        try {
            byte[] payloadBytes = urlDecoder.decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<HashMap<String, Object>>() {});

            Number expNumber = (Number) claims.get("exp");
            if (expNumber != null && Instant.now().getEpochSecond() > expNumber.longValue()) {
                throw new UnauthorizedException("JWT token has expired");
            }

            Object subObj = claims.get("sub");
            if (subObj == null) {
                throw new UnauthorizedException("JWT token missing subject claim");
            }

            Long userId = Long.valueOf(subObj.toString());
            String email = (String) claims.get("email");
            String role = (String) claims.getOrDefault("role", "ROLE_PASSENGER");

            return new AuthenticatedUser(userId, email, role);
        } catch (UnauthorizedException ue) {
            throw ue;
        } catch (Exception e) {
            throw new UnauthorizedException("Failed to parse JWT claims: " + e.getMessage());
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
