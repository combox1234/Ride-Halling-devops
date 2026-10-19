package com.ridehailing.notification.controller;

import com.ridehailing.notification.model.NotificationRecord;
import com.ridehailing.notification.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/rides/{rideId}")
    public ResponseEntity<List<NotificationRecord>> getNotificationsByRideId(@PathVariable Long rideId) {
        List<NotificationRecord> notifications = notificationRepository.findByRideId(rideId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping
    public ResponseEntity<List<NotificationRecord>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationRecord> getNotificationById(@PathVariable String id) {
        return notificationRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
