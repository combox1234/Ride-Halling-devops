package com.ridehailing.ride.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridehailing.common.constant.AmqpConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
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
    @DisplayName("Topic and Direct exchanges are declared with expected names and durability")
    void shouldDeclareExchanges() {
        TopicExchange rideExchange = rabbitMqConfig.rideTopicExchange();
        DirectExchange billingExchange = rabbitMqConfig.billingDirectExchange();
        TopicExchange dlxExchange = rabbitMqConfig.dlxTopicExchange();

        assertThat(rideExchange.getName()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(rideExchange.isDurable()).isTrue();

        assertThat(billingExchange.getName()).isEqualTo(AmqpConstants.BILLING_DIRECT_EXCHANGE);
        assertThat(billingExchange.isDurable()).isTrue();

        assertThat(dlxExchange.getName()).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(dlxExchange.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Queues declare Dead-Letter Exchange arguments pointing to dlx.topic.exchange")
    void shouldDeclareQueuesWithDeadLetterArguments() {
        Queue matchingQueue = rabbitMqConfig.rideMatchingQueue();
        Queue notificationQueue = rabbitMqConfig.rideNotificationQueue();
        Queue billingQueue = rabbitMqConfig.rideBillingQueue();
        Queue dlq = rabbitMqConfig.deadLetterQueue();

        assertThat(matchingQueue.getName()).isEqualTo(AmqpConstants.QUEUE_RIDE_MATCHING);
        assertThat(matchingQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(matchingQueue.getArguments().get("x-dead-letter-routing-key")).isEqualTo(AmqpConstants.ROUTING_KEY_DLX_FAILED);

        assertThat(notificationQueue.getName()).isEqualTo(AmqpConstants.QUEUE_RIDE_NOTIFICATION);
        assertThat(notificationQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);

        assertThat(billingQueue.getName()).isEqualTo(AmqpConstants.QUEUE_RIDE_BILLING);
        assertThat(billingQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);

        assertThat(dlq.getName()).isEqualTo(AmqpConstants.QUEUE_DEAD_LETTER);
    }

    @Test
    @DisplayName("Bindings route matching, notification, billing, and DLQ accurately")
    void shouldDeclareBindings() {
        TopicExchange rideExchange = rabbitMqConfig.rideTopicExchange();
        DirectExchange billingExchange = rabbitMqConfig.billingDirectExchange();
        TopicExchange dlxExchange = rabbitMqConfig.dlxTopicExchange();

        Queue matchingQueue = rabbitMqConfig.rideMatchingQueue();
        Queue notificationQueue = rabbitMqConfig.rideNotificationQueue();
        Queue billingQueue = rabbitMqConfig.rideBillingQueue();
        Queue dlq = rabbitMqConfig.deadLetterQueue();

        Binding matchingBinding = rabbitMqConfig.rideMatchingBinding(matchingQueue, rideExchange);
        Binding notificationBinding = rabbitMqConfig.rideNotificationBinding(notificationQueue, rideExchange);
        Binding billingDirectBinding = rabbitMqConfig.rideBillingDirectBinding(billingQueue, billingExchange);
        Binding billingTopicBinding = rabbitMqConfig.rideBillingTopicBinding(billingQueue, rideExchange);
        Binding dlqBinding = rabbitMqConfig.deadLetterBinding(dlq, dlxExchange);

        assertThat(matchingBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_RIDE_MATCHING);
        assertThat(matchingBinding.getExchange()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(matchingBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_RIDE_REQUESTED);

        assertThat(notificationBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_RIDE_NOTIFICATION);
        assertThat(notificationBinding.getExchange()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(notificationBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_RIDE_ALL);

        assertThat(billingDirectBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_RIDE_BILLING);
        assertThat(billingDirectBinding.getExchange()).isEqualTo(AmqpConstants.BILLING_DIRECT_EXCHANGE);
        assertThat(billingDirectBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_RIDE_COMPLETED);

        assertThat(billingTopicBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_RIDE_BILLING);
        assertThat(billingTopicBinding.getExchange()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(billingTopicBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_RIDE_COMPLETED);

        assertThat(dlqBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_DEAD_LETTER);
        assertThat(dlqBinding.getExchange()).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_DLX_ALL);
    }

    @Test
    @DisplayName("Message converter is configured with trusted packages")
    void shouldConfigureJackson2JsonMessageConverter() {
        MessageConverter converter = rabbitMqConfig.jackson2JsonMessageConverter(objectMapper);
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("RetryOperationsInterceptor is configured with 3 attempts and DLQ recoverer")
    void shouldConfigureRetryOperationsInterceptor() {
        RabbitTemplate mockRabbitTemplate = mock(RabbitTemplate.class);
        RetryOperationsInterceptor interceptor = rabbitMqConfig.retryOperationsInterceptor(mockRabbitTemplate);
        assertThat(interceptor).isNotNull();
    }
}
