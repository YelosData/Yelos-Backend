package com.yelos.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String AI_PROCESSING_QUEUE = "ai.processing.queue";
    public static final String AI_PROCESSING_EXCHANGE = "ai.processing.exchange";
    public static final String AI_PROCESSING_ROUTING_KEY = "ai.processing";

    @Bean
    public Queue aiProcessingQueue() {
        return QueueBuilder
                .durable(AI_PROCESSING_QUEUE)
                .build();
    }

    @Bean
    public DirectExchange aiProcessingExchange() {
        return new DirectExchange(AI_PROCESSING_EXCHANGE);
    }

    @Bean
    public Binding aiProcessingBinding() {
        return BindingBuilder
                .bind(aiProcessingQueue())
                .to(aiProcessingExchange())
                .with(AI_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}