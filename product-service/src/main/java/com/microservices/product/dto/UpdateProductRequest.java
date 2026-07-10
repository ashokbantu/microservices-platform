package com.microservices.product.dto;

import com.microservices.product.entity.Product;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    private String name;
    private String description;
    @DecimalMin("0.01") private BigDecimal price;
    private Product.Category category;
}
