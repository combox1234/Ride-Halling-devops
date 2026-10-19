package com.ridehailing.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.constant.AmqpConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMqConfig {

    @Value("${spring.rabbitmq.listener.simple.auto-startup:false}")
    private boolean autoStartup;

    // --- Exchanges ---

    @Bean
    public TopicExchange rideTopicExchange() {
        return ExchangeBuilder.topicExchange(AmqpConstants.RIDE_TOPIC_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public TopicExchange dlxTopicExchange() {
        return ExchangeBuilder.topicExchange(AmqpConstants.DLX_TOPIC_EXCHANGE)
            .durable(true)
            .build();
    }

    // --- Queues with DLQ arguments ---

    @Bean
    public Queue rideNotificationQueue() {
        return QueueBuilder.durable(AmqpConstants.QUEUE_RIDE_NOTIFICATION)
            .withArgument("x-dead-letter-exchange", AmqpConstants.DLX_TOPIC_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", AmqpConstants.ROUTING_KEY_DLX_FAILED)
            .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(AmqpConstants.QUEUE_DEAD_LETTER)
            .build();
    }

    // --- Bindings ---

    @Bean
    public Binding rideNotificationBinding(Queue rideNotificationQueue, TopicExchange rideTopicExchange) {
        return BindingBuilder.bind(rideNotificationQueue)
            .to(rideTopicExchange)
            .with(AmqpConstants.ROUTING_KEY_RIDE_ALL);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange dlxTopicExchange) {
        return BindingBuilder.bind(deadLetterQueue)
            .to(dlxTopicExchange)
            .with(AmqpConstants.ROUTING_KEY_DLX_ALL);
    }

    // --- Serialization & Message Converter ---

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.ridehailing.*", "java.util.*", "java.lang.*", "java.math.*", "java.time.*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    // --- Retry Policy & DLQ Recoverer ---

    @Bean
    public RetryOperationsInterceptor retryOperationsInterceptor(RabbitTemplate rabbitTemplate) {
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
            rabbitTemplate,
            AmqpConstants.DLX_TOPIC_EXCHANGE,
            AmqpConstants.ROUTING_KEY_DLX_FAILED
        );

        return RetryInterceptorBuilder.stateless()
            .maxAttempts(3)
            .backOffOptions(1000, 2.0, 10000)
            .recoverer(recoverer)
            .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        SimpleRabbitListenerContainerFactoryConfigurer configurer,
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter,
        RetryOperationsInterceptor retryOperationsInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAdviceChain(retryOperationsInterceptor);
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
