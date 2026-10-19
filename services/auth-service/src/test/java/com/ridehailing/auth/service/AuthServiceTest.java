package com.ridehailing.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.auth.dto.AuthResponse;
import com.ridehailing.auth.dto.LoginRequest;
import com.ridehailing.auth.dto.RegisterRequest;
import com.ridehailing.auth.dto.TokenValidationResponse;
import com.ridehailing.auth.model.UserRole;
import com.ridehailing.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private UserRepository userRepository;
    private JwtTokenService jwtTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        jwtTokenService = new JwtTokenService(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            3600000,
            new ObjectMapper(),
            new org.springframework.mock.env.MockEnvironment()
        );
        authService = new AuthService(userRepository, jwtTokenService);
    }

    @Test
    @DisplayName("Should successfully register a new passenger")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "pass1234", UserRole.ROLE_PASSENGER);
        AuthResponse response = authService.register(request);

        assertThat(response.userId()).isNotNull();
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.role()).isEqualTo(UserRole.ROLE_PASSENGER);
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "pass1234", UserRole.ROLE_PASSENGER);
        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Should login successfully with correct credentials")
    void shouldLoginSuccessfully() {
        RegisterRequest registerReq = new RegisterRequest("Jane Doe", "jane@example.com", "pass1234", UserRole.ROLE_PASSENGER);
        authService.register(registerReq);

        LoginRequest loginReq = new LoginRequest("jane@example.com", "pass1234");
        AuthResponse response = authService.login(loginReq);

        assertThat(response.token()).isNotBlank();
        assertThat(response.email()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectInvalidPassword() {
        RegisterRequest registerReq = new RegisterRequest("Jane Doe", "jane@example.com", "pass1234", UserRole.ROLE_PASSENGER);
        authService.register(registerReq);

        LoginRequest loginReq = new LoginRequest("jane@example.com", "wrongpassword");
        assertThatThrownBy(() -> authService.login(loginReq))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should validate active bearer token")
    void shouldValidateActiveToken() {
        RegisterRequest registerReq = new RegisterRequest("Jane Doe", "jane@example.com", "pass1234", UserRole.ROLE_PASSENGER);
        AuthResponse auth = authService.register(registerReq);

        TokenValidationResponse val = authService.validateToken("Bearer " + auth.token());
        assertThat(val.valid()).isTrue();
        assertThat(val.userId()).isEqualTo(auth.userId());
        assertThat(val.role()).isEqualTo(UserRole.ROLE_PASSENGER);
    }

    @Test
    @DisplayName("Should strictly forbid public registration with ROLE_ADMIN")
    void shouldRejectAdminRoleInPublicRegistration() {
        RegisterRequest adminRequest = new RegisterRequest("Malicious Admin", "admin@example.com", "secret123", UserRole.ROLE_ADMIN);

        assertThatThrownBy(() -> authService.register(adminRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ROLE_ADMIN cannot be self-assigned through public registration");

        assertThat(userRepository.findByEmail("admin@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Should default to ROLE_PASSENGER when role is omitted in public registration")
    void shouldDefaultToPassengerWhenRoleOmitted() {
        RegisterRequest defaultRoleReq = new RegisterRequest("Sam Normal", "sam.normal@example.com", "pass1234");
        AuthResponse response = authService.register(defaultRoleReq);

        assertThat(response.role()).isEqualTo(UserRole.ROLE_PASSENGER);
        assertThat(response.email()).isEqualTo("sam.normal@example.com");
    }

    @Test
    @DisplayName("Should permit valid public registration with ROLE_DRIVER")
    void shouldPermitDriverPublicRegistration() {
        RegisterRequest driverReq = new RegisterRequest("Dave Driver", "dave@example.com", "driver123", UserRole.ROLE_DRIVER);
        AuthResponse response = authService.register(driverReq);

        assertThat(response.role()).isEqualTo(UserRole.ROLE_DRIVER);
    }

    @Test
    @DisplayName("Should successfully provision administrator via trusted provisioning mechanism")
    void shouldProvisionAdminViaTrustedMechanism() {
        com.ridehailing.auth.model.UserAccount admin = authService.provisionAdmin("Root Admin", "root@example.com", "supersecret123");

        assertThat(admin.id()).isNotNull();
        assertThat(admin.role()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(admin.email()).isEqualTo("root@example.com");

        // Verify the provisioned admin can authenticate and receive a valid ROLE_ADMIN token
        AuthResponse loginRes = authService.login(new LoginRequest("root@example.com", "supersecret123"));
        assertThat(loginRes.role()).isEqualTo(UserRole.ROLE_ADMIN);

        TokenValidationResponse tokenVal = authService.validateToken("Bearer " + loginRes.token());
        assertThat(tokenVal.valid()).isTrue();
        assertThat(tokenVal.role()).isEqualTo(UserRole.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should reject admin provisioning with duplicate email")
    void shouldRejectDuplicateEmailInAdminProvisioning() {
        authService.provisionAdmin("First Admin", "admin.corp@example.com", "supersecret123");

        assertThatThrownBy(() -> authService.provisionAdmin("Second Admin", "admin.corp@example.com", "anothersecret"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email already registered");
    }
}
