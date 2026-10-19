package com.ridehailing.matching.config;

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
    @DisplayName("Matching service declares ride topic and dlx topic exchanges")
    void shouldDeclareExchanges() {
        TopicExchange rideExchange = rabbitMqConfig.rideTopicExchange();
        TopicExchange dlxExchange = rabbitMqConfig.dlxTopicExchange();

        assertThat(rideExchange.getName()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(dlxExchange.getName()).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
    }

    @Test
    @DisplayName("Matching queue is configured with dead letter exchange arguments")
    void shouldDeclareQueueWithDeadLetterArguments() {
        Queue matchingQueue = rabbitMqConfig.rideMatchingQueue();
        Queue dlq = rabbitMqConfig.deadLetterQueue();

        assertThat(matchingQueue.getName()).isEqualTo(AmqpConstants.QUEUE_RIDE_MATCHING);
        assertThat(matchingQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(matchingQueue.getArguments().get("x-dead-letter-routing-key")).isEqualTo(AmqpConstants.ROUTING_KEY_DLX_FAILED);
        assertThat(dlq.getName()).isEqualTo(AmqpConstants.QUEUE_DEAD_LETTER);
    }

    @Test
    @DisplayName("Matching binding routes ride.requested to q.ride.matching")
    void shouldDeclareBindings() {
        TopicExchange rideExchange = rabbitMqConfig.rideTopicExchange();
        TopicExchange dlxExchange = rabbitMqConfig.dlxTopicExchange();
        Queue matchingQueue = rabbitMqConfig.rideMatchingQueue();
        Queue dlq = rabbitMqConfig.deadLetterQueue();

        Binding matchingBinding = rabbitMqConfig.rideMatchingBinding(matchingQueue, rideExchange);
        Binding dlqBinding = rabbitMqConfig.deadLetterBinding(dlq, dlxExchange);

        assertThat(matchingBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_RIDE_MATCHING);
        assertThat(matchingBinding.getExchange()).isEqualTo(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        assertThat(matchingBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_RIDE_REQUESTED);

        assertThat(dlqBinding.getDestination()).isEqualTo(AmqpConstants.QUEUE_DEAD_LETTER);
        assertThat(dlqBinding.getExchange()).isEqualTo(AmqpConstants.DLX_TOPIC_EXCHANGE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(AmqpConstants.ROUTING_KEY_DLX_ALL);
    }

    @Test
    @DisplayName("Message converter is JSON with trusted packages")
    void shouldProvideJacksonMessageConverter() {
        MessageConverter converter = rabbitMqConfig.jackson2JsonMessageConverter(objectMapper);
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("Retry interceptor is configured for listener containers")
    void shouldConfigureRetryInterceptor() {
        RabbitTemplate mockRabbitTemplate = mock(RabbitTemplate.class);
        RetryOperationsInterceptor interceptor = rabbitMqConfig.retryOperationsInterceptor(mockRabbitTemplate);
        assertThat(interceptor).isNotNull();
    }
}
