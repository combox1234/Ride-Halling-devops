package com.ridehailing.passenger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.passenger.dto.CreatePassengerRequest;
import com.ridehailing.passenger.dto.UpdatePassengerRequest;
import com.ridehailing.passenger.model.PassengerProfile;
import com.ridehailing.passenger.repository.PassengerRepository;
import com.ridehailing.passenger.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PassengerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PassengerRepository passengerRepository;

    private String validTokenUser101;
    private String validTokenUser102;

    @BeforeEach
    void setUp() {
        passengerRepository.clear();
        validTokenUser101 = "Bearer " + createTestToken(101L, "alice@example.com", "ROLE_PASSENGER", 3600);
        validTokenUser102 = "Bearer " + createTestToken(102L, "bob@example.com", "ROLE_PASSENGER", 3600);
    }

    @Test
    @DisplayName("POST /api/v1/passengers should create profile when authenticated as same user")
    void shouldCreatePassengerProfile() throws Exception {
        CreatePassengerRequest req = new CreatePassengerRequest(101L, "Alice Johnson", "alice@example.com", "+1234567890");

        mockMvc.perform(post("/api/v1/passengers")
                .header("Authorization", validTokenUser101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Alice Johnson"))
            .andExpect(jsonPath("$.data.email").value("alice@example.com"))
            .andExpect(jsonPath("$.data.id").value(notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/passengers should reject unauthenticated request with 401")
    void shouldRejectUnauthenticatedCreation() throws Exception {
        CreatePassengerRequest req = new CreatePassengerRequest(101L, "Alice Johnson", "alice@example.com", "+1234567890");

        mockMvc.perform(post("/api/v1/passengers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v1/passengers should reject profile creation for different user ID with 403")
    void shouldRejectProfileCreationForDifferentUser() throws Exception {
        // Authenticated as 101, but attempting to create profile for user 102
        CreatePassengerRequest req = new CreatePassengerRequest(102L, "Bob", "bob@example.com", "+1987654321");

        mockMvc.perform(post("/api/v1/passengers")
                .header("Authorization", validTokenUser101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /api/v1/passengers/me should retrieve own profile")
    void shouldGetOwnProfileViaMeEndpoint() throws Exception {
        CreatePassengerRequest req = new CreatePassengerRequest(101L, "Alice", "alice@example.com", "+1234567890");
        passengerRepository.save(new PassengerProfile(null, 101L, req.name(), req.email(), req.phone(), 5.0, 0, Instant.now()));

        mockMvc.perform(get("/api/v1/passengers/me")
                .header("Authorization", validTokenUser101))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/passengers/me should reject mismatched/forged X-User-Id header with 403")
    void shouldRejectMismatchedXUserIdHeader() throws Exception {
        // Token belongs to user 101, but forged header asserts 102
        mockMvc.perform(get("/api/v1/passengers/me")
                .header("Authorization", validTokenUser101)
                .header("X-User-Id", "102"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").value(containsString("does not match authenticated token identity")));
    }

    @Test
    @DisplayName("IDOR prevention: GET /api/v1/passengers/{id} should return 403 when reading another user's profile")
    void shouldPreventIdorOnReadProfile() throws Exception {
        // Create profile for user 102
        PassengerProfile profile102 = passengerRepository.save(
            new PassengerProfile(null, 102L, "Bob", "bob@example.com", "+1987654321", 5.0, 0, Instant.now())
        );

        // User 101 attempts to read user 102's profile
        mockMvc.perform(get("/api/v1/passengers/" + profile102.id())
                .header("Authorization", validTokenUser101))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").value(containsString("not authorized to view")));
    }

    @Test
    @DisplayName("IDOR prevention: PUT /api/v1/passengers/{id} should return 403 when updating another user's profile")
    void shouldPreventIdorOnUpdateProfile() throws Exception {
        // Create profile for user 102
        PassengerProfile profile102 = passengerRepository.save(
            new PassengerProfile(null, 102L, "Bob", "bob@example.com", "+1987654321", 5.0, 0, Instant.now())
        );

        UpdatePassengerRequest updateReq = new UpdatePassengerRequest("Hacked Name", "+199999999");

        // User 101 attempts to update user 102's profile
        mockMvc.perform(put("/api/v1/passengers/" + profile102.id())
                .header("Authorization", validTokenUser101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PUT /api/v1/passengers/{id} should succeed when updating own profile")
    void shouldUpdateOwnProfile() throws Exception {
        PassengerProfile profile101 = passengerRepository.save(
            new PassengerProfile(null, 101L, "Alice", "alice@example.com", "+1234567890", 5.0, 0, Instant.now())
        );

        UpdatePassengerRequest updateReq = new UpdatePassengerRequest("Alice Updated", "+1999888777");

        mockMvc.perform(put("/api/v1/passengers/" + profile101.id())
                .header("Authorization", validTokenUser101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Alice Updated"))
            .andExpect(jsonPath("$.data.phone").value("+1999888777"));
    }

    @Test
    @DisplayName("GET /api/v1/passengers/{id} should return 404 when profile does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/passengers/999")
                .header("Authorization", validTokenUser101))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    private String createTestToken(Long userId, String email, String role, long validSeconds) {
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
            mac.init(new SecretKeySpec(JwtTokenValidator.DEFAULT_DEV_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = encoder.encodeToString(mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8)));

            return dataToSign + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
