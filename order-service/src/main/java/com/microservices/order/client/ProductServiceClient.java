package com.microservices.order.client;

import com.microservices.order.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", fallbackFactory = ProductServiceFallbackFactory.class)
public interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id,
                                   @RequestHeader("Authorization") String token);
    @PutMapping("/api/products/{id}/reserve-stock")
    void reserveStock(@PathVariable("id") Long id,
                      @RequestParam("quantity") int quantity,
                      @RequestHeader("Authorization") String token);
    @PutMapping("/api/products/{id}/release-stock")
    void releaseStock(@PathVariable("id") Long id,
                      @RequestParam("quantity") int quantity,
                      @RequestHeader("Authorization") String token);
}
