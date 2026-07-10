package com.microservices.notification.consumer;

import com.microservices.notification.dto.OrderEvent;
import com.microservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "order-events",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received order event: type={}, orderId={}, partition={}, offset={}",
            event.getEventType(), event.getOrderId(), partition, offset);

        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED"   -> notificationService.sendOrderConfirmation(event);
                case "ORDER_CANCELLED" -> notificationService.sendOrderCancellationNotification(event);
                case "ORDER_SHIPPED"   -> notificationService.sendShippingNotification(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.getEventType(), e.getMessage());
            throw e;
        }
    }
}
