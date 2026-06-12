package com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert;

import com.mcmp.o11ymanager.trigger.application.persistence.model.DirectAlert;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes a {@link DirectAlert} to the direct (manual) alert exchange so it flows through the
 * normal RabbitMQ consumer ({@code alert-manual.queue}) and is delivered to a single channel. Used
 * by the "Test" button to verify end-to-end notification delivery.
 */
@Component
public class DirectAlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.direct.exchangeName}")
    private String exchangeName;

    @Value("${spring.rabbitmq.direct.routingKey}")
    private String routingKey;

    public DirectAlertPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(DirectAlert directAlert) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, directAlert);
    }
}
