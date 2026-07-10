package com.microservices.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("status", "SERVICE_UNAVAILABLE",
                         "message", "Auth service is currently unavailable.",
                         "timestamp", LocalDateTime.now().toString())));
    }

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("status", "SERVICE_UNAVAILABLE",
                         "message", "User service is currently unavailable.",
                         "timestamp", LocalDateTime.now().toString())));
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("status", "SERVICE_UNAVAILABLE",
                         "message", "Order service is currently unavailable.",
                         "timestamp", LocalDateTime.now().toString())));
    }

    @GetMapping("/product")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("status", "SERVICE_UNAVAILABLE",
                         "message", "Product service is currently unavailable.",
                         "timestamp", LocalDateTime.now().toString())));
    }
}
