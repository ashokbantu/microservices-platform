package com.microservices.notification.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderEvent {
    private Long orderId;
    private String username;
    private String status;
    private BigDecimal totalAmount;
    private String eventType;
    private LocalDateTime timestamp;
}
