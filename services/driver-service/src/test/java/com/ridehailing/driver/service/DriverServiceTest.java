package com.ridehailing.driver.service;

import com.ridehailing.common.dto.DriverLocationPing;
import com.ridehailing.common.dto.DriverProfileDto;
import com.ridehailing.common.dto.DriverStatus;
import com.ridehailing.common.exception.AccessDeniedException;
import com.ridehailing.driver.dto.CreateDriverRequest;
import com.ridehailing.driver.repository.DriverRepository;
import com.ridehailing.driver.security.AuthenticatedUser;
import com.ridehailing.driver.store.RedisDriverLocationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverServiceTest {

    private DriverRepository driverRepository;
    private RedisDriverLocationStore locationStore;
    private DriverService driverService;

    private final AuthenticatedUser driverUser = new AuthenticatedUser(101L, "driver@example.com", "ROLE_DRIVER");
    private final AuthenticatedUser otherDriverUser = new AuthenticatedUser(102L, "other@example.com", "ROLE_DRIVER");
    private final AuthenticatedUser adminUser = new AuthenticatedUser(999L, "admin@example.com", "ROLE_ADMIN");

    @BeforeEach
    void setUp() {
        driverRepository = new DriverRepository();
        locationStore = new RedisDriverLocationStore(null);
        driverService = new DriverService(driverRepository, locationStore);
    }

    @Test
    @DisplayName("Should successfully register a new driver profile")
    void shouldRegisterDriver() {
        CreateDriverRequest request = new CreateDriverRequest(
            101L, "John Driver", "driver@example.com", "+1234567890",
            "Toyota", "Camry", "XYZ-789"
        );

        DriverProfileDto profile = driverService.registerDriver(request, driverUser);

        assertThat(profile.id()).isNotNull();
        assertThat(profile.userId()).isEqualTo(101L);
        assertThat(profile.name()).isEqualTo("John Driver");
        assertThat(profile.status()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(profile.rating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should reject driver registration for another user ID (IDOR prevention)")
    void shouldRejectRegistrationForDifferentUserId() {
        CreateDriverRequest request = new CreateDriverRequest(
            102L, "Malicious", "other@example.com", "+1234567890",
            "Toyota", "Camry", "XYZ-789"
        );

        assertThatThrownBy(() -> driverService.registerDriver(request, driverUser))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("You can only register a driver profile for your own authenticated user account");
    }

    @Test
    @DisplayName("Should reject viewing another driver's profile")
    void shouldRejectViewingAnotherDriverProfile() {
        CreateDriverRequest request = new CreateDriverRequest(
            101L, "John Driver", "driver@example.com", "+1234567890",
            "Toyota", "Camry", "XYZ-789"
        );
        DriverProfileDto created = driverService.registerDriver(request, driverUser);

        assertThatThrownBy(() -> driverService.getDriver(created.id(), otherDriverUser))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");

        // Admin should be allowed
        DriverProfileDto byAdmin = driverService.getDriver(created.id(), adminUser);
        assertThat(byAdmin.id()).isEqualTo(created.id());
    }

    @Test
    @DisplayName("Should update driver status and evict from location store when going OFFLINE")
    void shouldUpdateStatusAndEvictWhenOffline() {
        CreateDriverRequest request = new CreateDriverRequest(
            101L, "John Driver", "driver@example.com", "+1234567890",
            "Toyota", "Camry", "XYZ-789"
        );
        DriverProfileDto created = driverService.registerDriver(request, driverUser);

        // Transition to AVAILABLE
        DriverProfileDto available = driverService.updateStatus(created.id(), DriverStatus.AVAILABLE, driverUser);
        assertThat(available.status()).isEqualTo(DriverStatus.AVAILABLE);

        // Ping location
        DriverLocationPing ping = new DriverLocationPing(created.id(), 37.7749, -122.4194);
        driverService.updateLocation(ping, driverUser);
        assertThat(locationStore.hasActiveHeartbeat(created.id())).isTrue();

        // Transition to OFFLINE -> should evict
        DriverProfileDto offline = driverService.updateStatus(created.id(), DriverStatus.OFFLINE, driverUser);
        assertThat(offline.status()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(locationStore.hasActiveHeartbeat(created.id())).isFalse();
    }

    @Test
    @DisplayName("Should reject location update when driver is OFFLINE")
    void shouldRejectLocationUpdateWhenOffline() {
        CreateDriverRequest request = new CreateDriverRequest(
            101L, "John Driver", "driver@example.com", "+1234567890",
            "Toyota", "Camry", "XYZ-789"
        );
        DriverProfileDto created = driverService.registerDriver(request, driverUser);

        DriverLocationPing ping = new DriverLocationPing(created.id(), 37.7749, -122.4194);
        assertThatThrownBy(() -> driverService.updateLocation(ping, driverUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot update location for driver who is OFFLINE");
    }
}
