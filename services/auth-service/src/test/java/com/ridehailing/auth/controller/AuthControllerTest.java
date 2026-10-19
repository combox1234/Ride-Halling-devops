package com.ridehailing.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.auth.dto.LoginRequest;
import com.ridehailing.auth.dto.RegisterRequest;
import com.ridehailing.auth.model.UserRole;
import com.ridehailing.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.clear();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should create user and return 201")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("Sam Rider", "sam@example.com", "secure123", UserRole.ROLE_PASSENGER);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("sam@example.com"))
            .andExpect(jsonPath("$.data.role").value("ROLE_PASSENGER"))
            .andExpect(jsonPath("$.data.token").value(notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when validation fails")
    void shouldReturnBadRequestWhenRegistrationInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "short", null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should authenticate user and return 200")
    void shouldLoginSuccessfully() throws Exception {
        RegisterRequest regRequest = new RegisterRequest("Sam Rider", "sam@example.com", "secure123", UserRole.ROLE_PASSENGER);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)))
            .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("sam@example.com", "secure123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").value(notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/auth/validate should validate token")
    void shouldValidateBearerToken() throws Exception {
        RegisterRequest regRequest = new RegisterRequest("Sam Rider", "sam@example.com", "secure123", UserRole.ROLE_PASSENGER);
        String responseBody = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseBody).path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.valid").value(true))
            .andExpect(jsonPath("$.data.email").value("sam@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/validate should return 401 when token is missing or invalid")
    void shouldReturnUnauthorizedWhenTokenInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer invalid.token.value"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should reject attempt to register as ROLE_ADMIN")
    void shouldRejectRegistrationWithAdminRole() throws Exception {
        RegisterRequest adminRequest = new RegisterRequest("Malicious Admin", "admin.hack@example.com", "secret123", UserRole.ROLE_ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(containsString("ROLE_ADMIN cannot be self-assigned through public registration")));

        assertThat(userRepository.findByEmail("admin.hack@example.com")).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should default to ROLE_PASSENGER when role omitted in JSON")
    void shouldRegisterWithDefaultPassengerRoleWhenRoleOmitted() throws Exception {
        String jsonPayload = """
            {
                "name": "Default Passenger",
                "email": "default.passenger@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("default.passenger@example.com"))
            .andExpect(jsonPath("$.data.role").value("ROLE_PASSENGER"))
            .andExpect(jsonPath("$.data.token").value(notNullValue()));

        assertThat(userRepository.findByEmail("default.passenger@example.com")).isPresent();
        assertThat(userRepository.findByEmail("default.passenger@example.com").get().role())
            .isEqualTo(UserRole.ROLE_PASSENGER);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should reject ROLE_ADMIN even if JSON passes extra client fields")
    void shouldRejectAdminRoleEvenWithExtraFields() throws Exception {
        String jsonPayload = """
            {
                "name": "Privilege Escalator",
                "email": "escalate@example.com",
                "password": "password123",
                "role": "ROLE_ADMIN",
                "isAdmin": true,
                "admin": true
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(containsString("ROLE_ADMIN cannot be self-assigned through public registration")));

        assertThat(userRepository.findByEmail("escalate@example.com")).isEmpty();
    }
}
