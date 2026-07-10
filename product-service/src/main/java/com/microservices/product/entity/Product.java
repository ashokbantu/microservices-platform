package com.microservices.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_category", columnList = "category"),
    @Index(name = "idx_products_sku", columnList = "sku", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE products SET deleted = true WHERE id = ?")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(nullable = false) @Builder.Default private Integer stockQuantity = 0;
    @Column(nullable = false, unique = true, length = 50) private String sku;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Category category;
    @Column(nullable = false) @Builder.Default private boolean deleted = false;
    @Column(nullable = false) @Builder.Default private boolean active = true;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
    @Version private Long version;

    public enum Category {
        ELECTRONICS, CLOTHING, FOOD, SPORTS, BOOKS, HOME, BEAUTY, AUTOMOTIVE
    }
}
