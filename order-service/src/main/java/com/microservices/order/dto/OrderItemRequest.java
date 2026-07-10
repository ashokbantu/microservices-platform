package com.microservices.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull private Long productId;
    @NotNull @Min(1) private Integer quantity;
}
