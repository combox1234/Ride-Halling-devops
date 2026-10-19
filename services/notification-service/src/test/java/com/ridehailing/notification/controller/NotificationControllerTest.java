package com.ridehailing.notification.controller;

import com.ridehailing.notification.model.NotificationRecord;
import com.ridehailing.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("GET /api/v1/notifications/rides/{rideId} returns notifications for given ride")
    void shouldReturnNotificationsForRide() throws Exception {
        NotificationRecord record = new NotificationRecord(
            "notif-1",
            101L,
            202L,
            "PASSENGER",
            "RideRequestedEvent",
            "Your ride request #101 was received.",
            Instant.now(),
            "SENT"
        );

        when(notificationRepository.findByRideId(101L)).thenReturn(List.of(record));

        mockMvc.perform(get("/api/v1/notifications/rides/101")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("notif-1"))
            .andExpect(jsonPath("$[0].rideId").value(101))
            .andExpect(jsonPath("$[0].recipientType").value("PASSENGER"))
            .andExpect(jsonPath("$[0].message").value("Your ride request #101 was received."));
    }

    @Test
    @DisplayName("GET /api/v1/notifications returns all notifications")
    void shouldReturnAllNotifications() throws Exception {
        NotificationRecord record = new NotificationRecord(
            "notif-2",
            102L,
            303L,
            "DRIVER",
            "RideMatchedEvent",
            "Driver assigned",
            Instant.now(),
            "SENT"
        );

        when(notificationRepository.findAll()).thenReturn(List.of(record));

        mockMvc.perform(get("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("notif-2"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{id} returns single notification or 404")
    void shouldReturnNotificationById() throws Exception {
        NotificationRecord record = new NotificationRecord(
            "notif-3",
            103L,
            202L,
            "PASSENGER",
            "RideCompletedEvent",
            "Completed",
            Instant.now(),
            "SENT"
        );

        when(notificationRepository.findById("notif-3")).thenReturn(Optional.of(record));
        when(notificationRepository.findById("notif-unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/notifications/notif-3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("notif-3"));

        mockMvc.perform(get("/api/v1/notifications/notif-unknown"))
            .andExpect(status().isNotFound());
    }
}
