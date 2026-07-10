package com.microservices.product.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String,Object>> handleNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "status", 404, "message", e.getMessage(), "timestamp", LocalDateTime.now().toString()));
    }
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String,Object>> handleStock(InsufficientStockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "status", 409, "message", e.getMessage(), "timestamp", LocalDateTime.now().toString()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleGeneral(Exception e) {
        log.error("Unhandled: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "status", 500, "message", "Internal server error", "timestamp", LocalDateTime.now().toString()));
    }
}
