package com.microservices.product.repository;

import com.microservices.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndActiveTrue(Long id);
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByCategoryAndActiveTrue(Product.Category category, Pageable pageable);
    boolean existsBySku(String sku);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(Long id);
}
