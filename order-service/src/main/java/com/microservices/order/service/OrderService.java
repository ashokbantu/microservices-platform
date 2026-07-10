package com.microservices.order.service;

import com.microservices.order.client.ProductServiceClient;
import com.microservices.order.dto.*;
import com.microservices.order.entity.Order;
import com.microservices.order.entity.OrderItem;
import com.microservices.order.event.OrderEvent;
import com.microservices.order.exception.OrderNotFoundException;
import com.microservices.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String ORDER_TOPIC = "order-events";

    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    public OrderResponse createOrder(CreateOrderRequest request, String username, String token) {
        List<OrderItem> items = request.getItems().stream().map(itemReq -> {
            ProductResponse product = productServiceClient.getProductById(itemReq.getProductId(), token);
            productServiceClient.reserveStock(itemReq.getProductId(), itemReq.getQuantity(), token);
            BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            return OrderItem.builder()
                .productId(product.getId()).productName(product.getName())
                .quantity(itemReq.getQuantity()).unitPrice(product.getPrice()).totalPrice(totalPrice)
                .build();
        }).collect(Collectors.toList());

        BigDecimal totalAmount = items.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = Order.builder().username(username).items(items)
            .totalAmount(totalAmount).shippingAddress(request.getShippingAddress())
            .status(Order.OrderStatus.PENDING).build();
        items.forEach(i -> i.setOrder(order));
        Order saved = orderRepository.save(order);

        kafkaTemplate.send(ORDER_TOPIC, String.valueOf(saved.getId()), OrderEvent.builder()
            .orderId(saved.getId()).username(username).status("PENDING")
            .totalAmount(totalAmount).eventType("ORDER_CREATED").build());
        log.info("Order created: orderId={}, user={}", saved.getId(), username);
        return mapToResponse(saved);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id, String username) {
        return orderRepository.findByIdAndUsername(id, username)
            .map(this::mapToResponse)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    }

    public Page<OrderResponse> getOrdersByUser(String username, Pageable pageable) {
        return orderRepository.findByUsername(username, pageable).map(this::mapToResponse);
    }

    @CacheEvict(value = "orders", key = "#orderId")
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String username, String token) {
        Order order = orderRepository.findByIdAndUsername(orderId, username)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CONFIRMED)
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        order.getItems().forEach(item ->
            productServiceClient.releaseStock(item.getProductId(), item.getQuantity(), token));
        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        kafkaTemplate.send(ORDER_TOPIC, String.valueOf(orderId), OrderEvent.builder()
            .orderId(orderId).username(username).status("CANCELLED")
            .totalAmount(order.getTotalAmount()).eventType("ORDER_CANCELLED").build());
        return mapToResponse(saved);
    }

    public OrderResponse createOrderFallback(CreateOrderRequest request, String username, String token, Exception e) {
        log.error("Order creation failed (circuit open): {}", e.getMessage());
        throw new RuntimeException("Order service temporarily unavailable. Please try again.");
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
            .id(order.getId()).username(order.getUsername()).status(order.getStatus())
            .totalAmount(order.getTotalAmount()).shippingAddress(order.getShippingAddress())
            .createdAt(order.getCreatedAt())
            .items(order.getItems().stream().map(i -> OrderItemResponse.builder()
                .productId(i.getProductId()).productName(i.getProductName())
                .quantity(i.getQuantity()).unitPrice(i.getUnitPrice()).totalPrice(i.getTotalPrice())
                .build()).collect(Collectors.toList()))
            .build();
    }
}
