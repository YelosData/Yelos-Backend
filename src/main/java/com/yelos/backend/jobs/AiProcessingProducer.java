package com.yelos.backend.jobs;

import com.yelos.backend.config.RabbitMqConfig;
import com.yelos.backend.jobs.dto.AiProcessingMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiProcessingProducer {

    private final RabbitTemplate rabbitTemplate;

    public AiProcessingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(AiProcessingMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.AI_PROCESSING_EXCHANGE,
                RabbitMqConfig.AI_PROCESSING_ROUTING_KEY,
                message
        );
    }
}