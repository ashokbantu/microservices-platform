package com.microservices.notification.service;

import com.microservices.notification.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Async("notificationExecutor")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendOrderConfirmation(OrderEvent event) {
        log.info("Sending order confirmation for orderId={}, user={}", event.getOrderId(), event.getUsername());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getUsername() + "@example.com");
            message.setSubject("Order Confirmation - #" + event.getOrderId());
            message.setText(String.format(
                "Dear %s,\n\nYour order #%d has been placed successfully.\nTotal: $%.2f\n\nThank you!",
                event.getUsername(), event.getOrderId(), event.getTotalAmount()));
            mailSender.send(message);
            log.info("Order confirmation sent for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order confirmation: {}", e.getMessage());
            throw e;
        }
    }

    @Async("notificationExecutor")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendOrderCancellationNotification(OrderEvent event) {
        log.info("Sending cancellation notification for orderId={}", event.getOrderId());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getUsername() + "@example.com");
            message.setSubject("Order Cancelled - #" + event.getOrderId());
            message.setText(String.format(
                "Dear %s,\n\nYour order #%d has been cancelled.\nRefund of $%.2f will be processed within 5-7 business days.\n\nSorry for the inconvenience.",
                event.getUsername(), event.getOrderId(), event.getTotalAmount()));
            mailSender.send(message);
            log.info("Cancellation notification sent for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
            throw e;
        }
    }

    @Async("notificationExecutor")
    public void sendShippingNotification(OrderEvent event) {
        log.info("Sending shipping notification for orderId={}", event.getOrderId());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getUsername() + "@example.com");
        message.setSubject("Order Shipped - #" + event.getOrderId());
        message.setText(String.format(
            "Dear %s,\n\nYour order #%d has been shipped!\n\nThank you for shopping with us.",
            event.getUsername(), event.getOrderId()));
        mailSender.send(message);
    }
}
