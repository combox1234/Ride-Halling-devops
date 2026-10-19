package com.ridehailing.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridehailing.common.constant.AmqpConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMqConfigTest {

    private RabbitMqConfig rabbitMqConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        rabbitMqConfig = new RabbitMqConfig();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Notification service declares ride topic and dead-letter exchanges")
    void shouldDeclareExchanges() {
        TopicExchange rideExchange = rabbitMqConfig.rideTopicExchange();
        TopicExchange dlxExchange = rabbitMqConfig.dlxTopicExchange();

        assertThat(rideExchange.getName()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(dlxExchange.getName()).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
    }

    @Test
    @DisplayName("Notification queue declares dead-letter exchange arguments")
    void shouldDeclareQueueWithDeadLetterArguments() {
        Queue notificationQueue = rabbitMqConfig.rideNotificationQueue();
        Queue dlq = rabbitMqConfig.deadLetterQueue();

        assertThat(notificationQueue.getName()).isEqualTo(AmqpConstants.QUEUE_RIDE_NOTIFICATION);
        assertThat(notificationQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(notificationQueue.getArguments().get("x-dead-letter-routing-key")).isEqualTo(AmqpConstants.ROUTING_KEY_DLX_FAILED);
        assertThat(dlq.getName()).isEqualTo(AmqpConstants.QUEUE_DEAD_LETTER);
    }

    @Test
    @DisplayName("Notification binding subscribes to ride.* topic wildcard")
    void shouldDeclareBindings() {
        TopicExchange rideExchange = rabbitMqConfig.rideTopicExchange();
        TopicExchange dlxExchange = rabbitMqConfig.dlxTopicExchange();
        Queue notificationQueue = rabbitMqConfig.rideNotificationQueue();
        Queue dlq = rabbitMqConfig.deadLetterQueue();

        Binding notificationBinding = rabbitMqConfig.rideNotificationBinding(notificationQueue, rideExchange);
        Binding dlqBinding = rabbitMqConfig.deadLetterBinding(dlq, dlxExchange);

        assertThat(notificationBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_RIDE_NOTIFICATION);
        assertThat(notificationBinding.getExchange()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(notificationBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_RIDE_ALL);

        assertThat(dlqBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_DEAD_LETTER);
        assertThat(dlqBinding.getExchange()).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_DLX_ALL);
    }

    @Test
    @DisplayName("Message converter is Jackson2JsonMessageConverter")
    void shouldConfigureMessageConverter() {
        MessageConverter converter = rabbitMqConfig.jackson2JsonMessageConverter(objectMapper);
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("RetryOperationsInterceptor is configured with 3 attempts")
    void shouldConfigureRetryInterceptor() {
        RabbitTemplate mockRabbitTemplate = mock(RabbitTemplate.class);
        RetryOperationsInterceptor interceptor = rabbitMqConfig.retryOperationsInterceptor(mockRabbitTemplate);
        assertThat(interceptor).isNotNull();
    }
}
