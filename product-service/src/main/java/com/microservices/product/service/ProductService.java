package com.microservices.product.service;

import com.microservices.product.dto.*;
import com.microservices.product.entity.Product;
import com.microservices.product.exception.InsufficientStockException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.repository.ProductRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    @Bulkhead(name = "productService")
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product from DB: {}", id);
        return productRepository.findByIdAndActiveTrue(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }

    @Cacheable(value = "products_page", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @CircuitBreaker(name = "productService", fallbackMethod = "getAllProductsFallback")
    public Page<ProductResponse> getAllProducts(Pageable pageable, Product.Category category) {
        Page<Product> products = (category != null)
            ? productRepository.findByCategoryAndActiveTrue(category, pageable)
            : productRepository.findByActiveTrue(pageable);
        return products.map(this::mapToResponse);
    }

    @CacheEvict(value = {"products", "products_page"}, allEntries = true)
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku()))
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        Product product = Product.builder()
            .name(request.getName()).description(request.getDescription())
            .price(request.getPrice()).stockQuantity(request.getStockQuantity())
            .sku(request.getSku()).category(request.getCategory())
            .build();
        return mapToResponse(productRepository.save(product));
    }

    @CachePut(value = "products", key = "#id")
    @CacheEvict(value = "products_page", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void reserveStock(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        if (product.getStockQuantity() < quantity)
            throw new InsufficientStockException("Insufficient stock for product " + productId);
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    @Transactional
    public void releaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    @CacheEvict(value = {"products", "products_page"}, allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id))
            throw new ProductNotFoundException("Product not found: " + id);
        productRepository.deleteById(id);
    }

    public ProductResponse getProductFallback(Long id, Exception e) {
        log.warn("Circuit breaker fallback for getProductById({}): {}", id, e.getMessage());
        throw new ProductNotFoundException("Product service temporarily unavailable for: " + id);
    }

    public Page<ProductResponse> getAllProductsFallback(Pageable pageable, Product.Category category, Exception e) {
        log.warn("Circuit breaker fallback for getAllProducts: {}", e.getMessage());
        return Page.empty(pageable);
    }

    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
            .id(p.getId()).name(p.getName()).description(p.getDescription())
            .price(p.getPrice()).stockQuantity(p.getStockQuantity())
            .sku(p.getSku()).category(p.getCategory()).createdAt(p.getCreatedAt())
            .build();
    }
}
