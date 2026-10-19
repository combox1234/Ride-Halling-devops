package com.ridehailing.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NotificationServiceApplicationTest {

    @Test
    @DisplayName("Application context loads cleanly without external RabbitMQ dependency")
    void contextLoads() {
    }
}
