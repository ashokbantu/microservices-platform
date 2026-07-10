package com.microservices.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty private List<OrderItemRequest> items;
    @NotBlank private String shippingAddress;
}
