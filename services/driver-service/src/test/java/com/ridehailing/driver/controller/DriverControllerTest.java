package com.ridehailing.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.DriverLocationPing;
import com.ridehailing.common.dto.DriverStatus;
import com.ridehailing.driver.dto.CreateDriverRequest;
import com.ridehailing.driver.dto.UpdateDriverStatusRequest;
import com.ridehailing.driver.repository.DriverRepository;
import com.ridehailing.driver.security.JwtTokenValidator;
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
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DriverRepository driverRepository;

    private String validTokenDriver101;
    private String validTokenDriver102;

    @BeforeEach
    void setUp() {
        driverRepository.clear();
        validTokenDriver101 = "Bearer " + createTestToken(101L, "driver101@example.com", "ROLE_DRIVER", 3600);
        validTokenDriver102 = "Bearer " + createTestToken(102L, "driver102@example.com", "ROLE_DRIVER", 3600);
    }

    @Test
    @DisplayName("POST /api/v1/drivers should register driver when authenticated")
    void shouldRegisterDriver() throws Exception {
        CreateDriverRequest req = new CreateDriverRequest(
            101L, "David Miller", "driver101@example.com", "+15551234567",
            "Toyota", "Prius", "DRV-101"
        );

        mockMvc.perform(post("/api/v1/drivers")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.name").value("David Miller"))
            .andExpect(jsonPath("$.data.status").value("OFFLINE"));
    }

    @Test
    @DisplayName("POST /api/v1/drivers should reject registration without JWT")
    void shouldRejectRegistrationWithoutJwt() throws Exception {
        CreateDriverRequest req = new CreateDriverRequest(
            101L, "David Miller", "driver101@example.com", "+15551234567",
            "Toyota", "Prius", "DRV-101"
        );

        mockMvc.perform(post("/api/v1/drivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("PUT /api/v1/drivers/{id}/status should update driver status to AVAILABLE")
    void shouldUpdateDriverStatus() throws Exception {
        CreateDriverRequest req = new CreateDriverRequest(
            101L, "David Miller", "driver101@example.com", "+15551234567",
            "Toyota", "Prius", "DRV-101"
        );
        String createRes = mockMvc.perform(post("/api/v1/drivers")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();

        Long driverId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        UpdateDriverStatusRequest statusReq = new UpdateDriverStatusRequest(DriverStatus.AVAILABLE);

        mockMvc.perform(put("/api/v1/drivers/" + driverId + "/status")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/drivers/location should accept location ping when AVAILABLE")
    void shouldAcceptLocationPing() throws Exception {
        CreateDriverRequest req = new CreateDriverRequest(
            101L, "David Miller", "driver101@example.com", "+15551234567",
            "Toyota", "Prius", "DRV-101"
        );
        String createRes = mockMvc.perform(post("/api/v1/drivers")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();

        Long driverId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        // Set to AVAILABLE
        mockMvc.perform(put("/api/v1/drivers/" + driverId + "/status")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateDriverStatusRequest(DriverStatus.AVAILABLE))))
            .andExpect(status().isOk());

        // Location ping
        DriverLocationPing ping = new DriverLocationPing(driverId, 37.7749, -122.4194);
        mockMvc.perform(post("/api/v1/drivers/location")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ping)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("Driver location updated successfully")));
    }

    @Test
    @DisplayName("GET /api/v1/drivers/{id} should reject access from a different driver (ownership check)")
    void shouldRejectAccessFromDifferentDriver() throws Exception {
        CreateDriverRequest req = new CreateDriverRequest(
            101L, "David Miller", "driver101@example.com", "+15551234567",
            "Toyota", "Prius", "DRV-101"
        );
        String createRes = mockMvc.perform(post("/api/v1/drivers")
                .header("Authorization", validTokenDriver101)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();

        Long driverId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/drivers/" + driverId)
                .header("Authorization", validTokenDriver102))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    private String createTestToken(Long userId, String email, String role, long ttlSeconds) {
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

            long exp = Instant.now().getEpochSecond() + ttlSeconds;
            Map<String, Object> claims = Map.of(
                "sub", userId.toString(),
                "email", email,
                "role", role,
                "exp", exp
            );
            String payload = encoder.encodeToString(objectMapper.writeValueAsBytes(claims));

            String dataToSign = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(JwtTokenValidator.DEFAULT_DEV_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = encoder.encodeToString(mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8)));

            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test token", e);
        }
    }
}
