package com.ridehailing.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.auth.model.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtTokenService {

    public static final String DEFAULT_DEV_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final String secret;
    private final long expirationMs;
    private final ObjectMapper objectMapper;
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder urlDecoder = Base64.getUrlDecoder();

    @Autowired
    public JwtTokenService(
        @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
        @Value("${jwt.expiration-ms:86400000}") long expirationMs,
        ObjectMapper objectMapper,
        Environment environment
    ) {
        this.expirationMs = expirationMs;
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

    public String generateToken(UserAccount user) {
        try {
            long nowSec = Instant.now().getEpochSecond();
            long expSec = nowSec + (expirationMs / 1000);

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = Map.of(
                "sub", String.valueOf(user.id()),
                "email", user.email(),
                "role", user.role().name(),
                "iat", nowSec,
                "exp", expSec
            );

            String encodedHeader = urlEncoder.encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedPayload = urlEncoder.encodeToString(objectMapper.writeValueAsBytes(payload));
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(dataToSign);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }

    public Optional<Map<String, Object>> validateAndExtractClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String dataToSign = parts[0] + "." + parts[1];
        String expectedSignature = sign(dataToSign);

        // Constant-time signature verification
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            return Optional.empty();
        }

        try {
            byte[] payloadBytes = urlDecoder.decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<HashMap<String, Object>>() {});

            Number expNumber = (Number) claims.get("exp");
            if (expNumber != null && Instant.now().getEpochSecond() > expNumber.longValue()) {
                return Optional.empty(); // Token has expired
            }

            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public long getExpirationMs() {
        return expirationMs;
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
