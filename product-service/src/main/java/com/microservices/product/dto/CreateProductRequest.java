package com.microservices.product.dto;

import com.microservices.product.entity.Product;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotBlank private String name;
    private String description;
    @NotNull @DecimalMin("0.01") private BigDecimal price;
    @NotNull @Min(0) private Integer stockQuantity;
    @NotBlank private String sku;
    @NotNull private Product.Category category;
}
