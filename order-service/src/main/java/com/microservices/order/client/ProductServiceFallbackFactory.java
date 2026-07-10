package com.microservices.order.client;

import com.microservices.order.dto.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductServiceFallbackFactory implements FallbackFactory<ProductServiceClient> {
    @Override
    public ProductServiceClient create(Throwable cause) {
        return new ProductServiceClient() {
            @Override
            public ProductResponse getProductById(Long id, String token) {
                log.error("Product service unavailable for product {}: {}", id, cause.getMessage());
                return ProductResponse.builder().id(id).name("Service Unavailable").build();
            }
            @Override
            public void reserveStock(Long id, int quantity, String token) {
                log.error("Cannot reserve stock - product service unavailable: {}", cause.getMessage());
                throw new RuntimeException("Product service unavailable: " + cause.getMessage());
            }
            @Override
            public void releaseStock(Long id, int quantity, String token) {
                log.error("Cannot release stock - product service unavailable: {}", cause.getMessage());
            }
        };
    }
}
