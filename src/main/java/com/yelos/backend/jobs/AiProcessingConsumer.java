package com.yelos.backend.jobs;

import com.yelos.backend.config.RabbitMqConfig;
import com.yelos.backend.jobs.dto.AiProcessingMessage;
import com.yelos.backend.jobs.services.AiProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AiProcessingConsumer {

    private final AiProcessingService aiProcessingService;

    public AiProcessingConsumer(AiProcessingService aiProcessingService) {
        this.aiProcessingService = aiProcessingService;
    }

    @RabbitListener(queues = RabbitMqConfig.AI_PROCESSING_QUEUE)
    public void consume(AiProcessingMessage message) {
        try {
            aiProcessingService.process(message);
        } catch (Exception e) {
            System.out.println("AI processing failed for postId = " + message.postId());
            System.out.println("Reason = " + e.getMessage());
        }
    }
}